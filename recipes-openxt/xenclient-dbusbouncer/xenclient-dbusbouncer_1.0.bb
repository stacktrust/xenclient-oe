DESCRIPTION = "XenClient DBUS socket connections dom0-uivm"
LICENSE = "GPLv2"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/GPL-2.0;md5=801f80980d171dd6425610833a22dbe6"
DEPENDS = "libargo"
RDEPENDS_${PN} += "xen-libxenstore"

PACKAGE_ARCH = "${MACHINE_ARCH}"

SRC_URI = "file://dbusbouncer.c \
	   file://dbusbouncer.initscript \
"

INITSCRIPT_NAME = "dbusbouncer"
INITSCRIPT_PARAMS = "defaults 29"

S = "${WORKDIR}"

inherit update-rc.d xenclient

DEB_SUITE = "wheezy"
DEB_ARCH = "i386"

DEB_NAME = "xenclient-dbusbouncer"
DEB_DESC="Argo - UNIX socket proxy"
DEB_DESC_EXT="This package provides a proxy for remote socket access using Argo."
DEB_SECTION="misc"
DEB_PKG_MAINTAINER = "Citrix Systems <customerservice@citrix.com>"

LDFLAGS += "-largo -lxenstore"

ASNEEDED = ""

do_compile() {
	oe_runmake dbusbouncer
	# ${STRIP} dbusbouncer
}

do_install() {
	install -d ${D}${sbindir}
	install -m 0755 ${WORKDIR}/dbusbouncer ${D}${sbindir}
	install -d ${D}${sysconfdir}/init.d
	install -m 0755 ${WORKDIR}/dbusbouncer.initscript ${D}${sysconfdir}/init.d/dbusbouncer
}

DEBUG_BUILD = "1"
