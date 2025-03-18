# Copyright 2024 NXP
# SPDX-License-Identifier: BSD-3-Clause

include_guard()
message("microej/ecom-wifi component is included.")

target_sources(${MCUX_SDK_PROJECT_NAME} PRIVATE
    ${CMAKE_CURRENT_LIST_DIR}/src/ecom_wifi_helper_RW610.c
    ${CMAKE_CURRENT_LIST_DIR}/src/LLECOM_WIFI_impl.c
    ${CMAKE_CURRENT_LIST_DIR}/src/WIFI_RW610_driver.c
)

target_include_directories(${MCUX_SDK_PROJECT_NAME} PRIVATE
    ${CMAKE_CURRENT_LIST_DIR}/inc
    ${SdkMiddlewareDirPath}/wifi_nxp/incl
    ${SdkMiddlewareDirPath}/wifi_nxp/incl/port/os
    ${SdkMiddlewareDirPath}/wifi_nxp/wifi_bt_firmware
)
