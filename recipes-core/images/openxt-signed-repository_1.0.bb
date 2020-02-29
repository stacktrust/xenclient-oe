# Copyright (c) 2014, Citrix Systems
# Copyright (c) 2020, BAE Systems
# Released under the MIT license
#
# Created by Rich Persaud <rich.persaud@baesystems.com>
#     derived from Citrix XenClient do_build.sh
#
# Convert multiconfig VM and rootfs packages to a signed OpenXT package
# repository with this layout:
#
# packages.main/
# packages.main/control.tar.bz2
# packages.main/dom0-rootfs.ext3.gz
# packages.main/ndvm-rootfs.ext3.disk.vhd.gz
# packages.main/syncvm-rootfs.ext3.vhd.gz
# packages.main/uivm-rootfs.ext3.vhd.gz
# packages.main/OXT-PACKAGES
# packages.main/OXT-REPOSITORY
# packages.main/OXT-SIGNATURE
#
# TODO:
# 1.    Create test case which validates filesystem against manifest
#       of names and size ranges
# 2.    Create library of functions for OpenXT version artifact
#       generation, signing and validation, for use by multiple recipes
# 3.    Support external device for secret storage, e.g. HSM/smartcard for
#       production release signing

LICENSE = "MIT"
LIC_FILES_CHKSUM = 'file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302'

PV="1.0"

DEPENDS = " \
    openxt-mc-image-oxt-dom0 \
    openxt-mc-image-oxt-uivm \
    openxt-mc-image-oxt-ndvm \
    openxt-mc-image-oxt-syncvm \
    openxt-mc-image-oxt-installer \
    coreutils-native \
    openssl-native \
   "

COMPATIBLE_MACHINE = "(openxt-installer)"

FILES_${PN} = "${datadir}/${OXT_BRAND_LC}/packages.main"

#--- Generate OpenXT installer package repository
oxt_repo_create () {
    local PATH_REPO="$1"
    local FILE_REPO_HEADER="$2"
    local FILE_REPO_PACKAGES="$3"

    echo -n > "${FILE_REPO_PACKAGES}"

    # Parse manifest file with this structure:
    #   name format optional/required source_filename dest_path
    while read -r l
    do
        local name format opt_req src dest filesize sha256sum

	    name=$(echo "$l"    | awk '{print $1}')
	    format=$(echo "$l"  | awk '{print $2}')
	    opt_req=$(echo "$l" | awk '{print $3}')
	    src=$(echo "$l"     | awk '{print $4}')
	    dest=$(echo "$l"    | awk '{print $5}')

	    if [ ! -e "${PATH_REPO}/$src" ] ; then
            if [ "$opt_req" = "required" ] ; then
                echo "Error: Required file $src is missing"
                exit 1
            fi
            echo "Optional file $src is missing: skipping"
            continue
	    fi

        # add to repository package manifest
        (
	    cd "${PATH_REPO}" || exit
	    filesize=$( du -b "$src" | awk '{print $1}' )
	    sha256sum=$( sha256sum "$src" | awk '{print $1}' )

	    echo "$name" "$filesize" "$sha256sum" "$format" \
		     "$opt_req" "$src" "$dest" | tee -a "${FILE_REPO_PACKAGES}"
	    )

    done <<-'EOF'
control tarbz2 required control.tar.bz2 /
dom0 ext3gz required dom0-rootfs.ext3.gz /
uivm vhdgz required uivm-rootfs.ext3.vhd.gz /storage/uivm
ndvm vhdgz required ndvm-rootfs.ext3.disk.vhd.gz /storage/ndvm
syncvm vhdgz optional syncvm-rootfs.ext3.vhd.gz /storage/syncvm
file iso optional xc-tools.iso /storage/isos/xc-tools.iso
EOF

    PACKAGES_SHA256SUM=$(sha256sum "${FILE_REPO_PACKAGES}" |
			 awk '{print $1}')

    # Pad OXT-REPOSITORY to 1 MB with blank lines. If changed,
    # the repository-signing process will also need to change.
    {
        cat <<EOF
repository:main
pack:Base Platform
product:${OXT_BRAND_MC}
build:${OXT_ID}
version:${OXT_VERSION}
release:${OXT_RELEASE}
upgrade-from:${OXT_UPGRADEABLE_RELEASES}
packages:${PACKAGES_SHA256SUM}
EOF
        yes ""
    } | head -c 1048576 > "${FILE_REPO_HEADER}"
}

#--- Sign OpenXT package repository using private key
oxt_repo_sign () {
    local FILE_REPO_HEADER="$1"
    local FILE_REPO_SIGNATURE="$2"
    local FILE_CERTIFICATE="$3"
    local FILE_PRIVATE_KEY="$4"
    local ARG_PASSPHRASE="$5"

    openssl smime -sign \
                  -aes256 \
                  -binary \
                  -in "${FILE_REPO_HEADER}" \
                  -out "${FILE_REPO_SIGNATURE}" \
                  -outform PEM \
                  -signer "${FILE_CERTIFICATE}" \
                  -inkey "${FILE_PRIVATE_KEY}" \
                  "${ARG_PASSPHRASE}" ||
        bbfatal "Error generating package repository signature"
}

#--- Collect sysroot multiconfig rootfs into repo directory
do_install () {
    local PATH_ROOTFS="${STAGING_DATADIR}/${OXT_BRAND_LC}/rootfs"
    local PATH_OXT_REPO="${D}${datadir}/${OXT_BRAND_LC}/packages.main"
    local FILE_REPO_HEADER="${PATH_OXT_REPO}/OXT-REPOSITORY"
    local FILE_REPO_PACKAGES="${PATH_OXT_REPO}/OXT-PACKAGES"
    local FILE_REPO_SIGNATURE="${PATH_OXT_REPO}/OXT-SIGNATURE"

    install -d "${PATH_OXT_REPO}"
    install -m 0444 "${PATH_ROOTFS}/dom0-rootfs.ext3.gz" \
                    "${PATH_OXT_REPO}"
    install -m 0444 "${PATH_ROOTFS}/ndvm-rootfs.ext3.disk.vhd.gz" \
                     "${PATH_OXT_REPO}"
    install -m 0444 "${PATH_ROOTFS}/uivm-rootfs.ext3.vhd.gz" \
                    "${PATH_OXT_REPO}"
    install -m 0444 "${PATH_ROOTFS}/syncvm-rootfs.ext3.vhd.gz" \
                    "${PATH_OXT_REPO}"
    install -m 0444 "${PATH_ROOTFS}/control.tar.bz2" \
                    "${PATH_OXT_REPO}"

    oxt_repo_create  "${PATH_OXT_REPO}" \
                     "${FILE_REPO_HEADER}" \
                     "${FILE_REPO_PACKAGES}"

    oxt_repo_sign    "${FILE_REPO_HEADER}" \
                     "${FILE_REPO_SIGNATURE}" \
                     "${OXT_REPO_SIGNING_CERT}" \
                     "${OXT_REPO_SIGNING_KEY}" \
                     "${OXT_REPO_SIGNING_KEY_PASSPHRASE}"
}

# Skip unused tasks for small performance gain
do_fetch[noexec] = "1"
do_unpack[noexec] = "1"
do_patch[noexec] = "1"
do_configure[noexec] = "1"
do_compile[noexec] = "1"

#--- For non-mc builds, skip this recipe when BBMULTICONFIG is empty
# Confirm presence of required variables
python () {
    if not (d.getVar('BBMULTICONFIG')):
        raise bb.parse.SkipRecipe("Repository assembly requires multiconfig inputs")
    vars = ['OXT_REPO_SIGNING_CERT',
            'OXT_REPO_SIGNING_KEY',
            'OXT_ID',
            'OXT_VERSION',
            'OXT_RELEASE',
            'OXT_UPGRADEABLE_RELEASES']
    for var in vars:
       if not (d.getVar(var)):
           bb.fatal("Please define variable %s in local.conf" % var)
}
