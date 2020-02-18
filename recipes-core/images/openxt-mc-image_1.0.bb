# Copyright (C) 2020, BAE Systems, Inc.
# Released under the MIT license
#
# Created by Rich Persaud <rich.persaud@baesystems.com>
#     derived from openembedded-core multiconfig-test-parse.bb
#     by Joshua Watt <JPEWhacker@gmail.com>
#
# Package cross-machine multiconfig deploy image artifacts for use in
# target sysroot. Multiple recipes are generated from this template.
#
# Global input variables:
#     OXT_BRAND_LC
#     BBMULTICONFIG
#     TMPDIR
#
# Example Usage:
#     DEPENDS += " openxt-mc-image-oxt-dom0 "
#     where "oxt-dom0" is a multiconfig target defined in BBMULTICONFIG
#     and multiconfig file oxt-dom0.conf

LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

PV="1.0"

inherit mcextend

#--- Define multiconfig parameter sets for recipe generation
# MCNAME variable is reserved by OE core mcextend.bbclass. This recipe
# is referenced as openxt-mc-image-${mcname}. Each set of definitions
# below specifies properties for one MCNAME and generated recipe

BBCLASSEXTEND += " mcextend:oxt-dom0 "
MC_TMP_virtclass-mcextend-oxt-dom0 = "dom0"
MC_SRC_virtclass-mcextend-oxt-dom0 = "dom0"
MC_DEST_virtclass-mcextend-oxt-dom0 = "dom0-rootfs"
MC_EXT_virtclass-mcextend-oxt-dom0 = "ext3.gz"
MC_MACHINE_virtclass-mcextend-oxt-dom0 = "xenclient-dom0"

BBCLASSEXTEND += " mcextend:oxt-uivm "
MC_TMP_virtclass-mcextend-oxt-uivm = "uivm"
MC_SRC_virtclass-mcextend-oxt-uivm = "uivm"
MC_DEST_virtclass-mcextend-oxt-uivm = "uivm-rootfs"
MC_EXT_virtclass-mcextend-oxt-uivm = "ext3.vhd.gz"
MC_MACHINE_virtclass-mcextend-oxt-uivm = "xenclient-uivm"

BBCLASSEXTEND += " mcextend:oxt-ndvm "
MC_TMP_virtclass-mcextend-oxt-ndvm = "ndvm"
MC_SRC_virtclass-mcextend-oxt-ndvm = "ndvm"
MC_DEST_virtclass-mcextend-oxt-ndvm = "ndvm-rootfs"
MC_EXT_virtclass-mcextend-oxt-ndvm = "ext3.disk.vhd.gz"
MC_MACHINE_virtclass-mcextend-oxt-ndvm = "xenclient-ndvm"

BBCLASSEXTEND += " mcextend:oxt-syncvm "
MC_TMP_virtclass-mcextend-oxt-syncvm = "syncvm"
MC_SRC_virtclass-mcextend-oxt-syncvm = "syncvm"
MC_DEST_virtclass-mcextend-oxt-syncvm = "syncvm-rootfs"
MC_EXT_virtclass-mcextend-oxt-syncvm = "ext3.vhd.gz"
MC_MACHINE_virtclass-mcextend-oxt-syncvm = "xenclient-syncvm"

BBCLASSEXTEND += " mcextend:oxt-installer "
MC_TMP_virtclass-mcextend-oxt-installer = "installer"
MC_SRC_virtclass-mcextend-oxt-installer = "installer-part2"
MC_DEST_virtclass-mcextend-oxt-installer = "control"
MC_EXT_virtclass-mcextend-oxt-installer = "tar.bz2"
MC_MACHINE_virtclass-mcextend-oxt-installer = "openxt-installer"

#--- Substitute multiconfig parameters to generate multiple recipes
PROVIDES = "${PN}-${MCNAME}"

FILES_${PN} = "${datadir}/${OXT_BRAND_LC}/rootfs/${MC_DEST}.${MC_EXT}"

# Reuse TMPDIR 'base' until OE implements multiconfig-aware $TMPDIR
# - copy from Machine A deploy to Target B sysroot
# - skip full $TMPDIR since it defaults to Machine B (oxt-installer)
# - preserve TMPDIR base which may point to a ramdisk
MC_DEPLOY_DIR_IMAGE = "${TMPDIR}/../tmp-${MC_TMP}-glibc/deploy/images/${MC_MACHINE}"

do_install () {
    install -d "${D}${datadir}/${OXT_BRAND_LC}/rootfs"
    install -m 0444 "${MC_DEPLOY_DIR_IMAGE}/xenclient-${MC_SRC}-image-${MC_MACHINE}.${MC_EXT}" \
                    "${D}${datadir}/${OXT_BRAND_LC}/rootfs/${MC_DEST}.${MC_EXT}"
}

# Define cross-machine dependency on rootfs image completion. The "::"
# syntax is a placeholder for multiconfig Target B (oxt-installer)
# from openxt-signed-repository and openxt-installer-iso recipes
do_install[mcdepends] += "mc::${MCNAME}:xenclient-${MC_SRC}-image:do_image_complete"

# Skip unused tasks for small performance gain
do_fetch[noexec] = "1"
do_unpack[noexec] = "1"
do_patch[noexec] = "1"
do_configure[noexec] = "1"
do_compile[noexec] = "1"

#--- For non-mc builds, skip this recipe when BBMULTICONFIG is empty
python () {
    mcname = d.getVar('MCNAME')
    if not (mcname and mcname.strip()):
        raise bb.parse.SkipRecipe("Not a multiconfig target")
    multiconfigs = d.getVar('BBMULTICONFIG') or ""
    if mcname not in multiconfigs:
        raise bb.parse.SkipRecipe("Multiconfig target %s not enabled" % mcname)
    if not (d.getVar('OXT_BRAND_LC')):
        bb.fatal("Please define OXT_BRAND_LC in local.conf")
}
