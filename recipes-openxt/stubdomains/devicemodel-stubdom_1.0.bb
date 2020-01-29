SUMMARY = "Recipe for packaging qemu stubdomain image"
HOMEPAGE = "https://openxt.org"
LICENSE = "GPLv2"
LIC_FILES_CHKSUM ?= "file://${COMMON_LICENSE_DIR}/GPL-2.0;md5=801f80980d171dd6425610833a22dbe6"

STUBDOMAIN_DIR = "${STUBDOM_DEPLOY_DIR}/images/${STUBDOMAIN_MACHINE}"
STUBDOMAIN_NAME = "xenclient-stubdomain-initramfs-image"
STUBDOMAIN_IMAGE = "${STUBDOMAIN_DIR}/${STUBDOMAIN_NAME}-${STUBDOMAIN_MACHINE}.cpio.gz"

do_install() {
        install -d ${D}${libdir}/xen/boot
        install -m 0644 ${STUBDOMAIN_IMAGE} \
            ${D}${libdir}/xen/boot/stubdomain-initramfs
}

do_fetch() {
        if [ ! -e "${STUBDOMAIN_IMAGE}" ]; then
                bbfatal "The stubdomain image, ${STUBDOMAIN_IMAGE}, must be built first"
        fi
}

FILES_${PN} = "${libdir}/xen/boot/stubdomain-initramfs"

RDEPENDS_${PN} = "${STUBDOMAIN_MACHINE}-kernel"

# Support both multiconfig and standard builds. For non-mc builds,
# this dependency can be enforced by a wrapper shell script.
# When the build becomes exclusively multiconfig, drop this recipe
# and have dom0-image mcdepend+copy stubdom-initramfs-image
python() {
    if d.getVar('BBMULTICONFIG'):
        d.appendVarFlag('do_fetch', 'mcdepends', 'mc:oxt-dom0:oxt-stubdomain:xenclient-stubdomain-initramfs-image:do_image_complete')
        d.setVar('STUBDOM_DEPLOY_DIR',
                 d.getVar('TMPDIR') + "/../tmp-stubdomain-glibc/deploy")
    else:
        d.setVar('STUBDOM_DEPLOY_DIR', d.getVar('DEPLOY_DIR'))
}
