/*
 *  Copyright 2024 NXP
 *  SPDX-License-Identifier: BSD-3-Clause
 */

/* @TEST_ANCHOR */
#define WIFI_BOARD_RW610
/* @END_TEST_ANCHOR */

#if defined(WIFI_BOARD_RW610)
#define RW610
#define FRDMRW610
#define WIFI_BT_USE_IMU_INTERFACE
#define WIFI_BT_TX_PWR_LIMITS "wlan_txpwrlimit_cfg_WW_rw610.h"
#define CONFIG_BT_SNOOP
#else
#error "Please define macro for RW610 board"
#endif
