DESCRIPTION = "OpenXT repository certificates"
LICENSE = "GPLv2"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/GPL-2.0;md5=801f80980d171dd6425610833a22dbe6"

SRC_URI = "file://${OXT_REPO_PROD_SIGNING_CERT} \
           file://${OXT_REPO_DEV_SIGNING_CERT} \
           file://verify-repo-metadata"

FILES_${PN} = "${datadir}/xenclient/repo-certs \
               ${bindir}/verify-repo-metadata"

inherit allarch

do_install() {
    CERTDIR_PROD=${D}${datadir}/xenclient/repo-certs/prod
    CERTDIR_DEV=${D}${datadir}/xenclient/repo-certs/dev
    install -d ${CERTDIR_PROD}
    install -d ${CERTDIR_DEV}

    install -m 0644 ${WORKDIR}/${OXT_REPO_PROD_SIGNING_CERT} ${CERTDIR_PROD}/cert.pem
    install -m 0644 ${WORKDIR}/${OXT_REPO_DEV_SIGNING_CERT} ${CERTDIR_DEV}/cert.pem

    install -d ${D}${bindir}
    install -m 0755 ${WORKDIR}/verify-repo-metadata ${D}${bindir}/
}

RDEPENDS_${PN} += " \
    openssl-bin \
"
