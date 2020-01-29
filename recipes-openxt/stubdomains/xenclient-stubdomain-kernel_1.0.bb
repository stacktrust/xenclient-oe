SUMMARY = "Recipe for packaging stubdomain kernel"
HOMEPAGE = "https://openxt.org"
LICENSE = "GPLv2"
LIC_FILES_CHKSUM ?= "file://${COMMON_LICENSE_DIR}/GPL-2.0;md5=801f80980d171dd6425610833a22dbe6"

STUBDOMAIN_DIR = "${STUBDOM_DEPLOY_DIR}/images/${STUBDOMAIN_MACHINE}"
STUBDOMAIN_KERNEL_BIN = "${STUBDOMAIN_DIR}/${STUBDOMAIN_KERNEL}-${STUBDOMAIN_MACHINE}.bin"

FILESEXTRAPATHS_prepend := "${STUBDOMAIN_DIR}:"

do_install() {
        install -d ${D}${libdir}/xen/boot
        install -m 0644 ${STUBDOMAIN_KERNEL_BIN} \
            ${D}${libdir}/xen/boot/stubdomain-bzImage
}

do_fetch() {
        if [ ! -e "${STUBDOMAIN_KERNEL_BIN}" ]; then
                bbfatal "The stubdomain kernel ${STUBDOMAIN_KERNEL_BIN} must be built first"
        fi
}

FILES_${PN} = "${libdir}/xen/boot/stubdomain-bzImage"

# Support both multiconfig and standard builds. For non-mc builds,
# this dependency can be enforced by a wrapper shell script.
# When the build becomes exclusively multiconfig, drop this recipe
# and have dom0-image mcdepend+copy stubdom linux-kernel
python() {
    if d.getVar('BBMULTICONFIG'):
        d.appendVarFlag('do_fetch', 'mcdepends',
                        'mc:oxt-dom0:oxt-stubdomain:linux-openxt:do_package')
        d.setVar('STUBDOM_DEPLOY_DIR',
                 d.getVar('TMPDIR') + "/../tmp-stubdomain-glibc/deploy")
    else:
        d.setVar('STUBDOM_DEPLOY_DIR', d.getVar('DEPLOY_DIR'))
}
