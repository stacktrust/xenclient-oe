# Copyright (c) 2020, BAE Systems
# Released under the MIT license
#
# Created by Rich Persaud <rich.persaud@baesystems.com>
#
# Use upstream OE image classes to configure and build OpenXT
# installer ISO. Since this depends on a package generated by
# multiconfig, building this recipe will execute a full OpenXT build.
#
# Example usage:
#   MACHINE=openxt-installer bitbake mc::openxt-installer-iso

LICENSE = "MIT"
LIC_FILES_CHKSUM = 'file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302'

PV="1.0"

DEPENDS += " \
    openxt-signed-repository \
   "

COMPATIBLE_MACHINE = "(openxt-installer)"

#--- Override image-live.bbclass defaults
IMAGE_FSTYPES += "iso"
BOOTIMG_VOLUME_ID = "${OXT_BRAND_UC}"
export IMAGE_BASENAME = "base"
ROOTFS=""
INITRD_IMAGE_LIVE="xenclient-installer-image"

#--- Override syslinux.bbclass defaults
AUTOSYSLINUXMENU = "0"
SYSLINUX_SERIAL = ""
SYSLINUX_TIMEOUT = "30"
SYSLINUX_ALLOWOPTIONS = "1"
SYSLINUX_PROMPT = "1"
SYSLINUX_KERNEL_ARGS=""
SYSLINUX_MULTIBOOT = "1"

#--- Define cmdlines for multiboot components
XEN_SERIAL  = "serial=115200,8n1,0x3f8 logging=serial,memory"
XEN_SERIAL += " com1=115200,8n1,pci console=com1"
XEN_HYPERTHREADING = "smt=0"
MB_XEN_GENERIC  = "xen.gz flask=disabled dom0_max_vcpus=1"
MB_XEN_GENERIC += " dom0_mem=max:8G ${XEN_HYPERTHREADING}"
MB_XEN_GENERIC += " ${XEN_SERIAL}"
MB_XEN_INTEL = "${MB_XEN_GENERIC} ucode=-1"

LINUX_SERIAL = "console=ttyS0,115200n8"
MB_LINUX_EFI_ANS  = "answerfile=/install/answers/uefi.ans"
MB_LINUX_BIOS_ANS = "answerfile=/install/answers/default.ans"
MB_LINUX1 = "/bzImage quiet root=/dev/ram"
MB_LINUX2 = "rw eject_cdrom=1 ${LINUX_SERIAL} selinux=0"
MB_LINUX3 = " start_install=new ${MB_LINUX_BIOS_ANS}"
MB_LINUX_BIOS  = "${MB_LINUX1} ${MB_LINUX2} ${MB_LINUX3}"
MB_LINUX_EFI  = "${MB_LINUX2} start_install=new ${MB_LINUX_EFI_ANS}"

MB_TBOOT  = "tboot.gz min_ram=0x2000000 loglvl=all ${SERIAL_XEN}"

MB_ACM = "gm45.acm --- q35.acm --- q45q43.acm --- duali.acm ---"
MB_ACM += " quadi.acm --- ivb_snb.acm --- xeon56.acm --- xeone7.acm"
MB_ACM += " --- hsw.acm --- bdw.acm --- skl.acm --- kbl.acm ---"
MB_ACM += " cfl.acm"

LABELS_LIVE = "intel amd generic"

APPEND_intel  = "${MB_TBOOT} --- ${MB_XEN_INTEL} --- ${MB_LINUX_BIOS}"
APPEND_intel += " --- ${MB_ACM} --- microcode_intel.bin"
APPEND_generic="${MB_XEN_GENERIC} --- ${MB_LINUX_BIOS}"
APPEND_amd="${APPEND_generic}"

APPEND_grub_intel   = "${MB_LINUX_EFI}"
APPEND_grub_generic = "${MB_LINUX_EFI}"
APPEND_grub_amd     = "${MB_LINUX_EFI}"

#--- Inherit upstream OE classes for live/iso image generation
# * image-live via image and IMAGE_FSTYPES=iso
# * syslinux for legacy bios boot, grub-efi for EFI boot
# MACHINE_FEATURES must include "pcbios efi"
inherit image syslinux grub-efi

syslinux_iso_populate_append() {
    #--- FIXME: move TXT populate to syslinux append in TXT/acm recipe
    #         : move xen populate to syslinux append in xen recipe
    local bootdirfiles="xen.gz \
                        tboot.gz *.acm license-*.txt \
                        microcode_intel.bin"
    for files in ${bootdirfiles}
    do
        install -m 0444 "${DEPLOY_DIR_IMAGE}"/${files} \
                        "${ISODIR}${ISOLINUXDIR}"
    done

    #--- VM+installer rootfs from openxt-signed-repository
    local ISO_REPODIR="${ISODIR}/packages.main"
    local SYSROOT_REPODIR="${STAGING_DATADIR}/${OXT_BRAND_LC}/packages.main"
    install -d      "${ISO_REPODIR}"
    install -m 0444 "${SYSROOT_REPODIR}"/* \
                    "${ISO_REPODIR}"
}

#--- For non-mc builds, skip this recipe when BBMULTICONFIG is empty
# Confirm presence of required variables
python () {
    if not (d.getVar('BBMULTICONFIG')):
        raise bb.parse.SkipRecipe("Installer ISO requires multiconfig inputs")
    for brand_var in ['OXT_BRAND_LC', 'OXT_BRAND_UC']:
       if not (d.getVar(brand_var)):
           bb.fatal("Please define variable %s in local.conf" % brand_var)
}
