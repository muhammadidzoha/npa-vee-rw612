# NXP Platform Accelerator for RW612 Freedom v1.0.0

This project is used to build NXP Platform Accelerator for the [RW612 Freedom](https://www.nxp.com/design/design-center/development-boards-and-designs/FRDM-RW612) with a display panel [LCD_PAR_S035](https://www.nxp.com/design/design-center/development-boards/general-purpose-mcus/3-5-480x320-ips-tft-lcd-module:LCD-PAR-S035).

<p float="left">
  <img src="Documentation/pictures/RW612/frdm-rw612.jpg" width="300" />
  <img src="Documentation/pictures/common/lcd-par-s035.jpg" width="300" /> 
</p>

NXP Platform Accelerator is a VEE (Virtual Execution Environment) that provides a hardware abstraction to develop applications in high-level programming languages such as Java.

NXP Platform Accelerator is built upon [MicroEJ technology](https://www.microej.com/product/vee/).

## Description

### Board Technical Specifications

|                         |               |
| ----------------------- | ------------- |
|MCU part number          |RW612          |
|MCU architecture         |Arm Cortex-M33 |
|MCU max clock frequency  |260 MHz        |
|Internal RAM size        |1.2MB          |
|External flash size      |2MB            |
|Display                  |480x320 LCD    |
|Ethernet interface       |yes            |
|WiFi interface           |yes            |
|eMMC/SD support          |via extension board|

### BSP (Board Support Package)

The BSP is based on the following versions:

* [MCUXpresso SDK](https://mcuxpresso.nxp.com/en/welcome) `2.16.000` for RW612 Freedom board
* [FreeRTOS](https://www.freertos.org/index.html) version `11.0.1`

### VEE Port Specifications

The architecture version is `8.1.0`.

This VEE Port provides the following Foundation Libraries:

|Foundation Library|Version|
|------------------|-------|
|BON               |1.4    |
|DEVICE            |1.2    |
|DRAWING           |1.0    |
|ECOM-NETWORK      |2.1    |
|ECOM-WIFI         |2.2    |
|EDC               |1.3    |
|MICROUI           |3.1    |
|NET               |1.1    |
|SECURITY          |1.7    |
|SNI               |1.4    |
|SSL               |2.2    |
|TRACE             |1.1    |

This VEE Port is compatible with MicroEJ SDK6 or higher.

## Requirements

* A PC with Windows 10 or higher or Linux (tested on Debian 11),
  * Note for Mac users: this documentation does not cover MacOS use; however, it is supported by the MicroEJ tools. If you are interested in Mac support, please [contact MicroEJ](https://www.microej.com/contact/#form_2).
* An internet connection to use the [MicroEJ Central Repository](https://developer.microej.com/central-repository/),
* An RW612 Freedom board (can be ordered [here](https://www.nxp.com/design/design-center/development-boards-and-designs/FRDM-RW612)) and LCD_PAR_S035 display panel (can be ordered [here](https://www.nxp.com/design/design-center/development-boards/general-purpose-mcus/3-5-480x320-ips-tft-lcd-module:LCD-PAR-S035))
* Optionally: a J-Link Debugger probe to flash the software.

## Preliminary Steps

### Get MicroEJ SDK

The MICROEJ SDK allows to build the VEE Port and the high-level applications.
It can be used to run the RW612 Freedom simulator.

* Install a Java Development Kit (JDK). The MICROEJ SDK requires a JDK (Java Development Kit) 11 or higher.
  You can download a JDK at [Java SE 11](https://www.oracle.com/java/technologies/downloads/#java11).
* Install MICROEJ SDK 6 by following the [SDK 6 Installation Documentation](https://docs.microej.com/en/latest/SDK6UserGuide/install.html).
  You can use your preferred IDE to work with the MICROEJ SDK.

This release has been tested with a JDK 11 and 17.

### Get the Necessary Tools for MCUXpresso SDK

You can choose to install MCUXpresso SDK tools manually or with the installer tool.

#### With the MCUXpresso Installer Tool

Install the MCUXpresso Installer tool with the following documentation: [link](https://github.com/nxp-mcuxpresso/vscode-for-mcux/wiki/Dependency-Installation).

Open it and install the following tools that are used during compilation/debugging:

* MCUXpresso SDK Developer (installs CMake, Ninja, West, and ARM GNU Toolchain)
* LinkServer
* SEGGER J-Link (if optional J-Link is used)

Also, install the following tools manually:

* [Make](https://gnuwin32.sourceforge.net/packages/make.htm), in version 3.81 or higher.
* On Ubuntu 22.04 or lower, [CMake](https://cmake.org/download/) must be installed manually to get the latest version (3.27 or higher needed).

#### Manual Installation

The MCUXpresso installer tool provides a convenient way to install these tools, but they can also be installed independently.
In the case of a standalone installation, the following versions need to be installed:

* [CMake](https://cmake.org/download/) version 3.27 or higher
* [Make](https://gnuwin32.sourceforge.net/packages/make.htm) version 3.81 or higher
* [ARM GNU Toolchain](https://developer.arm.com/downloads/-/arm-gnu-toolchain-downloads) version 13.2.1 or higher
* [LinkServer](https://www.nxp.com/design/design-center/software/development-software/mcuxpresso-software-and-tools-/linkserver-for-microcontrollers:LINKERSERVER) version 1.6.133 or higher

#### Environment Variables

Whatever procedure is used for the installation, the following environment variables must be configured:

* ``ARMGCC_DIR``: the installation directory of ARM GNU Toolchain, for example ``C:\Program Files (x86)\Arm GNU Toolchain arm-none-eabi\13.2 Rel1``
* If you use LinkServer to flash your device, add to ``PATH`` the installation directory of LinkServer, for example ``C:\NXP\LinkServer_24.12.21``.

> Note: On Windows, editing the User variables may not work —Preferably edit System variables.


## Fetch the Source Code

Clone the repository with the following commands:

```bash
mkdir npa-vee-rw612
cd npa-vee-rw612
west init -m https://github.com/nxp-mcuxpresso/npa-vee-rw612 .
west update
```

The following repositories will be created:

```text
.west npa-vee-rw612
```

> Note: On Windows, your path to the repository folder should be as short as possible

> Note: Your path should not contain a whitespace or special character

## Open the Project

Launch your IDE chosen during MicroEJ installation step and open the project folder (`npa-vee-rw612`). The following screenshots show Visual Studio Code, but similar results can be obtained with another IDE.

<p float="left">
  <img src="Documentation/pictures/common/sdk_import.png" width="300" />
</p>

Then wait for your IDE to finish loading the project, as indicated in the message in the status bar:

<p float="left">
  <img src="Documentation/pictures/common/sdk_open_loading.png" width="300" />
</p>

Once loaded, you should see the following files and folders:

<p float="left">
  <img src="Documentation/pictures/common/sdk_project_structure.png" width="300" />
</p>

And the Gradle view should look like this:

<p float="left">
  <img src="Documentation/pictures/common/sdk_gradle_view.png" width="300" />
</p>

## Build and Run an Application in Simulation Mode

### Choose your Demo Application

Following MicroEJ applications are included in this release.

* The `HelloWorld` application displays "Hello World" periodically. More details in [this README](apps/HelloWorld/README.md).
* The `SimpleGFX` application displays three moving rectangles using the [MicroUI API](https://docs.microej.com/en/latest/ApplicationDeveloperGuide/UI/MicroUI/index.html#section-app-microui). The coordinates of the rectangles are calculated in C native functions. More details in [this README](apps/simpleGFX/README.md).

### Execute runOnSimulator Task

To run an application in simulation mode, go to the Gradle view, expand the tasks of the chosen demo project, then double-click on the `microej` > `runOnSimulator` task:

<p float="left">
  <img src="Documentation/pictures/common/sdk_run_as_microej_app.png" width="300" />
</p>


The runOnSimulator task also builds the VEE Port declared as dependency if required.

## Build and Run Applications on your Board

### Board Setup

<p float="left">
<img src="Documentation/pictures/RW612/frdm-rw612-setup.jpg" width="500" />
</p>

Setup the FRDM-RW612
* Check that JP1 is open and JP4 is shorted to enable MCU-Link SWD feature
* Connect the USB-C cable to J10 to power the board.
* You can connect the display (in SPI mode, LCD-PAR-S035 dip switches (SW1) are set to ON, ON and ON) to J7.

The USB connection is used as a serial console for the SoC, as a CMSIS-DAP debugger, and as a power input for the board.

The MicroEJ VEE Port uses the virtual UART from the RW612 Freedom USB port. A COM port is automatically mounted when the board is plugged into a computer using a USB cable. All board logs are available through this COM port.

The COM port uses the following parameters:

| Baudrate | Data bits bits | Parity bits | Stop bits | Flow control |
| -------- | -------- | -------- | -------- | -------- |
| 115200     | 8     | None     | 1     | None     |

Debugger options

The FRDM-RW612 can either be flashed and connected to a debugger through the USB port J10, or through the SWD connector P1.

Once your setup is done, you can continue this Readme at the [Build and Deploy using MicroEJ SDK 6](#build-and-deploy-using-microej-sdk-6) section.

### Build and Deploy

There are two different ways to build and deploy an executable on target:

* Using MicroEJ SDK 6: this method allows to build and deploy an executable in one click using gradle tasks.
* Using MCUXpresso for VSCode: this method allows to debug on target in addition to build and deploy.

#### Build and Deploy using MicroEJ SDK 6


##### Configure debug or release mode

To configure the debug or release mode, change the `CHOSEN_MODE` variable in [set_project_env.bat](bsp/vee/scripts/set_project_env.bat) or [set_project_env.sh](bsp/vee/scripts/set_project_env.sh):

* Set it to `0` for debug mode
* Set it to `1` for release mode

##### Configure the debug probe

To configure the debug probe, change the `CHOSEN_PROBE` variable in [set_project_env.bat](bsp/vee/scripts/set_project_env.bat) or [set_project_env.sh](bsp/vee/scripts/set_project_env.sh):

* Set it to `flash` for a J-Link probe
* Set it to `flash_cmsisdap` for board internal probe

##### Configure the BSP Features

Compilation flags are located in [CMakePresets.json](bsp/vee/scripts/armgcc/CMakePresets.json).
To enable any desired features, please edit the file.

For changes in this file to take effect, the script [clean.bat](bsp/vee/scripts/clean.bat) or [clean.sh](bsp/vee/scripts/clean.sh) must be called.

##### Launch `runOnDevice` gradle task

To build and deploy your executable on your board, go to the Gradle view, expand the tasks of the chosen demo project, then double-click on the `microej` > `runOnDevice` task:

If you don't have a MicroEJ license, you will get an error message telling you to get one. Please follow [the instructions from MicroEJ](https://docs.microej.com/en/latest/SDK6UserGuide/licenses.html#evaluation-licenses) to get an evaluation license. To switch to a production license, please get in touch with your NXP representative.

This task calls:

* [build.bat](bsp/vee/scripts/build.bat) or [build.sh](bsp/vee/scripts/build.sh) to build the BSP,
* [run.bat](bsp/vee/scripts/run.bat) or [run.sh](bsp/vee/scripts/run.sh) script to flash the program on the target without opening a debug session.

<p float="left">
  <img src="Documentation/pictures/common/sdk_run_on_device.png" width="300" />
</p>

#### Build and Deploy using MCUXpresso for VSCode

MCUXpresso for VSCode is a plugin on VSCode IDE that builds and deploys firmware on NXP targets. It also provides the configuration to launch a debug session.

##### Generate MicroEJ Object Files

You need to produce the object files of the MicroEJ Application and deploy them into the BSP, but you will delegate the build of the Executable to MCUXpresso. To do so, follow these steps:

* Open the file `configuration/common.properties` located in your application folder and change the property `deploy.bsp.microejscript` to `false`
* Open the Gradle view, expand the tasks of your application, and double-click on the `microej` > `buildExecutable` task.

The following MicroEJ object files will be deployed in the BSP:

* The MicroEJ application (`microejapp.o`) will be deployed in `bsp/vee/lib`.
* The MicroEJ library (`microejruntime.a`) will be deployed in `bsp/vee/lib`.
* The MicroEJ header files (`*.h`) will be deployed in `bsp/vee/inc`.

##### Load the Project into VS Code

Open the project with VSCode (from where the .vscode is located).
Install [MCUXpresso for VSCode](https://www.nxp.com/design/design-center/software/embedded-software/mcuxpresso-for-visual-studio-code:MCUXPRESSO-VSC) plugin.

##### Select a Preset

Open the Command Palette (`CTRL + SHIFT + p`) and run `CMake: Select Configure Preset` to select the build mode you wish to use.

By default, you can select the `debug` variant.

<p float="left">
  <img src="Documentation/pictures/common/vscode_select_preset.jpg" width="500" />
</p>

You can also select a preset by using the Projects section from the "MCUXpresso for VS code" view and setting the appropriate build as default.

<p float="left">
  <img src="Documentation/pictures/common/vscode_projects_view.jpg" width="500" />
</p>

##### Configure the Project

Open the Command Palette (`CTRL + SHIFT + p`) and run `CMake: Configure`.

<p float="left">
  <img src="Documentation/pictures/common/vscode_select_configure.jpg" width="500" />
</p>

##### Configure the BSP Features

Compilation flags are located in [CMakePresets.json](bsp/vee/scripts/armgcc/CMakePresets.json).
To enable any desired features, please edit the file (and reload preset & re-configure if needed).

After a change in this file, a Pristine build must be made (see [Build the Project](#build-the-project) step).

##### Build the Project

Open the Command Palette (`CTRL + SHIFT + p`) and run `CMake: Build`.

<p float="left">
  <img src="Documentation/pictures/common/vscode_select_build.jpg" width="500" />
</p>

You can connect VS Code to the board using the Serial Link USB or a SEGGER J-Link probe.
Follow the [Board Setup](#board-setup) step for more information on how to connect the different debuggers.

Debug sessions can be started by pressing the `F5` key.

It is also possible to build and debug the project via the MCUXpresso plugin:

Right-click on the project, then:

* `Build Selected` or `Pristine Build/Rebuild Selected` to compile.
* `Debug` to debug (it can take some time before launching). In this case, the name of the selected preset must contain `debug` and not `release`.

<p float="left">
  <img src="Documentation/pictures/common/vscode_mcuxpr_build_debug.jpg" width="300" />
</p>

Once the firmware is flashed, you should see the application running on the target.

<ins>Note:</ins>
In case of connection issues to the target, reset the debug probe selection via the MCUXpresso plugin:

* Select the MCUXpresso plugin in the activity bar.
* Right-click on the project name and select `Reset Probe Selection`.
* Start the debug again.

<p float="left">
  <img src="Documentation/pictures/common/vscode_reset_probe_selection.jpg" width="300" />
</p>

## Optional Features


### Ethernet

Ethernet can be enabled or disabled by changing `ENABLE_ETHERNET` value in [CMakePresets.json](bsp/vee/scripts/armgcc/CMakePresets.json).
Set it to 1 to enable it and 0 to disable it.
Call [clean.bat](bsp/vee/scripts/clean.bat) or [clean.sh](bsp/vee/scripts/clean.sh) after changing this value.
Regarding the naming of this new interface, it will follow this syntax en\<index> where index is the relative position
among network interfaces enabled by the VEE Port.
### Wifi

Wifi can be enabled or disabled by changing `ENABLE_WIFI` value in [CMakePresets.json](bsp/vee/scripts/armgcc/CMakePresets.json).
Set it to 1 to enable it and 0 to disable it.
Call [clean.bat](bsp/vee/scripts/clean.bat) or [clean.sh](bsp/vee/scripts/clean.sh) after changing this value.
Regarding the naming of this new interface, it will follow this syntax ml\<index> where index is the relative position
among network interfaces enabled by the VEE Port.
### System View

For information about System View, please visit [SEGGER website](https://www.segger.com/products/development-tools/systemview/) or [MicroEJ documentation](https://docs.microej.com/en/latest/VEEPortingGuide/systemView.html#microej-core-engine-os-task).

Follow these steps to run a System View live analysis:

* Set `ENABLE_SYSTEM_VIEW` CMake variable to 1 in [CMakePresets.json](bsp/vee/scripts/armgcc/CMakePresets.json).
* Call [clean.bat](bsp/vee/scripts/clean.bat) or [clean.sh](bsp/vee/scripts/clean.sh).
* Set `FLASH_CMD` to `flash` in [set_project_env.bat](bsp/vee/scripts/set_project_env.bat) or [set_project_env.sh](bsp/vee/scripts/set_project_env.sh).
* Execute `runOnDevice` gradle task. Use a J-Link probe to flash your target.
* Open System View PC application
* Go to Target > Start Recording
* Select the following Recorder Configuration:
  * J-Link Connection = USB
  * Target Connection = RW612
  * Target Interface = SWD
  * Interface Speed (kHz) = 4000
  * RTT Control Block Detection = Auto
* Click Ok

If you have an issue, please have a look at the [Troubleshooting section](https://docs.microej.com/en/latest/VEEPortingGuide/systemView.html#troubleshooting) in MicroEJ documentation.

### Build constraint

By default, this VEE Port enables wifi and ethernet features by enabling both flags to 1.
It is mandatory to have at least one of these two flags enabled to properly build this VEE Port.

## Troubleshooting

### Setup Error

#### West Update and "Filename too long" Issue

On Windows, fetching the source code may trigger the following fatal error:
```error: unable to create file [...]: Filename too long.```

To avoid this, git configuration needs to be updated to handle long file names:

Start Git Bash as Administrator.

Run the following command:
```git config --system core.longpaths true```

#### West Update and "PermissionError: [WinError 5] Access is denied" Issue

If you get the error `PermissionError: [WinError 5] Access is denied`, please consider the following procedure :

```bash
rm .west
cd npa-vee-rw612
west init -l
cd ..
west update
```

### Ninja errors during BSP build

#### Ninja: error: loading 'build.ninja': The system cannot find the file specified

If you get the following error during the BSP build, please remove the cmake cache by running the script [clean.bat](bsp/vee/scripts/clean.bat) or [clean.sh](bsp/vee/scripts/clean.sh).

```text
"Failed to build the firmware"
ninja: error: loading 'build.ninja': The system cannot find the file specified.
make: *** [remake] Error 1
```



