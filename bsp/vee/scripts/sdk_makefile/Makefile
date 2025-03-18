#
# Copyright 2023-2024 NXP
#
# SPDX-License-Identifier: BSD-3-Clause
#
TARGET=npavee
DEVICE=RW612
LS_DEVICE=RW612:RD-RW612-QFN
ADDRESS=0x18000000
FLAVOUR=debug

ifeq ($(strip $(RELEASE)), 1)
	FLAVOUR=release
endif

BUILD_DIR=../armgcc/$(FLAVOUR)

include ../../common/sdk_makefile/Makefile
