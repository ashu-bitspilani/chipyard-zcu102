package sifive.fpgashells.shell.xilinx

import chisel3._
import chisel3.experimental.{attach, Analog, IO}
import freechips.rocketchip.config._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.tilelink._
import freechips.rocketchip.util.SyncResetSynchronizerShiftReg
import sifive.fpgashells.clocks._
import sifive.fpgashells.shell._
import sifive.fpgashells.ip.xilinx._
import sifive.blocks.devices.chiplink._
import sifive.fpgashells.devices.xilinx.xilinxzcu106mig._
import sifive.fpgashells.devices.xilinx.xdma._
import sifive.fpgashells.ip.xilinx.xxv_ethernet._

class SysClockZCU106PlacedOverlay(val shell: ZCU106ShellBasicOverlays, name: String, val designInput: ClockInputDesignInput, val shellInput: ClockInputShellInput)
  extends LVDSClockInputXilinxPlacedOverlay(name, designInput, shellInput)
{
  val node = shell { ClockSourceNode(freqMHz = 300, jitterPS = 50)(ValName(name)) }

  shell { InModuleBody {
    shell.xdc.addPackagePin(io.p, "AH12")
    shell.xdc.addPackagePin(io.n, "AJ12")
    shell.xdc.addIOStandard(io.p, "DIFF_SSTL12")
    shell.xdc.addIOStandard(io.n, "DIFF_SSTL12")
  } }
}
class SysClockZCU106ShellPlacer(shell: ZCU106ShellBasicOverlays, val shellInput: ClockInputShellInput)(implicit val valName: ValName)
  extends ClockInputShellPlacer[ZCU106ShellBasicOverlays]
{
    def place(designInput: ClockInputDesignInput) = new SysClockZCU106PlacedOverlay(shell, valName.name, designInput, shellInput)
}

class SDIOZCU106PlacedOverlay(val shell: ZCU106ShellBasicOverlays, name: String, val designInput: SPIDesignInput, val shellInput: SPIShellInput)
  extends SDIOXilinxPlacedOverlay(name, designInput, shellInput)
{
  shell { InModuleBody {
    // According to sdio.v, the assignment is -
    // cmd <- mosi
    // dat[0] -> miso
    // dat[3] <- cs
    // sd_clk <- spi_clk
    
    // And this matches PMOD pinout (https://digilent.com/shop/pmod-microsd-microsd-card-slot/) -
    // 1: cs/dat[3]    7: dat[1]
    // 2: mosi/cmd     8: dat[2]
    // 3: miso/dat[0]  9: CD
    // 4: sck          10: NC
    // 5: GND          11: GND
    // 6: VCC 3.3V     12: VCC 3.3V

    val packagePinsWithPackageIOs = Seq(("E20", IOPin(io.spi_clk)),
                                        ("A23", IOPin(io.spi_cs)), // This is labelled as cs, but is actually cmd
                                        ("F25", IOPin(io.spi_dat(0))),
                                        ("K24", IOPin(io.spi_dat(1))),
                                        ("L23", IOPin(io.spi_dat(2))),
                                        ("B23", IOPin(io.spi_dat(3))))

    packagePinsWithPackageIOs foreach { case (pin, io) => {
      shell.xdc.addPackagePin(io, pin)
      shell.xdc.addIOStandard(io, "LVCMOS18") // There is a level translator on the ZCU106 board to convert this to 3.3V
    } }
    packagePinsWithPackageIOs drop 1 foreach { case (pin, io) => {
      shell.xdc.addPullup(io)
      shell.xdc.addIOB(io)
    } }
  } }
}
class SDIOZCU106ShellPlacer(shell: ZCU106ShellBasicOverlays, val shellInput: SPIShellInput)(implicit val valName: ValName)
  extends SPIShellPlacer[ZCU106ShellBasicOverlays] {
  def place(designInput: SPIDesignInput) = new SDIOZCU106PlacedOverlay(shell, valName.name, designInput, shellInput)
}

class UARTZCU106PlacedOverlay(val shell: ZCU106ShellBasicOverlays, name: String, val designInput: UARTDesignInput, val shellInput: UARTShellInput)
  extends UARTXilinxPlacedOverlay(name, designInput, shellInput, true)
{
  shell { InModuleBody {
    val packagePinsWithPackageIOs = Seq(("AM15", IOPin(io.ctsn.get)),
                                        ("AP17", IOPin(io.rtsn.get)),
                                        ("AH17", IOPin(io.rxd)),
                                        ("AL17", IOPin(io.txd)))

    packagePinsWithPackageIOs foreach { case (pin, io) => {
      shell.xdc.addPackagePin(io, pin)
      shell.xdc.addIOStandard(io, "LVCMOS12") // 1.2V, part of PL DDR4 bank and there's a level translator
      shell.xdc.addIOB(io)
    } }
  } }
}
class UARTZCU106ShellPlacer(shell: ZCU106ShellBasicOverlays, val shellInput: UARTShellInput)(implicit val valName: ValName)
  extends UARTShellPlacer[ZCU106ShellBasicOverlays] {
  def place(designInput: UARTDesignInput) = new UARTZCU106PlacedOverlay(shell, valName.name, designInput, shellInput)
}

object LEDZCU106PinConstraints {
  val pins = Seq("AL11", "AL13", "AK13", "AE15", "AM8", "AM9", "AM10", "AM11")
}
class LEDZCU106PlacedOverlay(val shell: ZCU106ShellBasicOverlays, name: String, val designInput: LEDDesignInput, val shellInput: LEDShellInput)
  extends LEDXilinxPlacedOverlay(name, designInput, shellInput, packagePin = Some(LEDZCU106PinConstraints.pins(shellInput.number)), ioStandard = "LVCMOS12")
class LEDZCU106ShellPlacer(shell: ZCU106ShellBasicOverlays, val shellInput: LEDShellInput)(implicit val valName: ValName)
  extends LEDShellPlacer[ZCU106ShellBasicOverlays] {
  def place(designInput: LEDDesignInput) = new LEDZCU106PlacedOverlay(shell, valName.name, designInput, shellInput)
}

object ButtonZCU106PinConstraints {
  val pins = Seq("AG13", "AC14", "AK12", "AP20", "AL10")
}
class ButtonZCU106PlacedOverlay(val shell: ZCU106ShellBasicOverlays, name: String, val designInput: ButtonDesignInput, val shellInput: ButtonShellInput)
  extends ButtonXilinxPlacedOverlay(name, designInput, shellInput, packagePin = Some(ButtonZCU106PinConstraints.pins(shellInput.number)), ioStandard = "LVCMOS12")
class ButtonZCU106ShellPlacer(shell: ZCU106ShellBasicOverlays, val shellInput: ButtonShellInput)(implicit val valName: ValName)
  extends ButtonShellPlacer[ZCU106ShellBasicOverlays] {
  def place(designInput: ButtonDesignInput) = new ButtonZCU106PlacedOverlay(shell, valName.name, designInput, shellInput)
}

object SwitchZCU106PinConstraints {
  val pins = Seq("A17", "A16", "B15", "B15")
}
class SwitchZCU106PlacedOverlay(val shell: ZCU106ShellBasicOverlays, name: String, val designInput: SwitchDesignInput, val shellInput: SwitchShellInput)
  extends SwitchXilinxPlacedOverlay(name, designInput, shellInput, packagePin = Some(SwitchZCU106PinConstraints.pins(shellInput.number)), ioStandard = "LVCMOS18")
class SwitchZCU106ShellPlacer(shell: ZCU106ShellBasicOverlays, val shellInput: SwitchShellInput)(implicit val valName: ValName)
  extends SwitchShellPlacer[ZCU106ShellBasicOverlays] {
  def place(designInput: SwitchDesignInput) = new SwitchZCU106PlacedOverlay(shell, valName.name, designInput, shellInput)
}

class JTAGDebugBScanZCU106PlacedOverlay(val shell: ZCU106ShellBasicOverlays, name: String, val designInput: JTAGDebugBScanDesignInput, val shellInput: JTAGDebugBScanShellInput)
  extends JTAGDebugBScanXilinxPlacedOverlay(name, designInput, shellInput)
class JTAGDebugBScanZCU106ShellPlacer(val shell: ZCU106ShellBasicOverlays, val shellInput: JTAGDebugBScanShellInput)(implicit val valName: ValName)
  extends JTAGDebugBScanShellPlacer[ZCU106ShellBasicOverlays] {
  def place(designInput: JTAGDebugBScanDesignInput) = new JTAGDebugBScanZCU106PlacedOverlay(shell, valName.name, designInput, shellInput)
}

case object ZCU106DDRSize extends Field[BigInt](0x40000000L * 2) // 2GB
class DDRZCU106PlacedOverlay(val shell: ZCU106ShellBasicOverlays, name: String, val designInput: DDRDesignInput, val shellInput: DDRShellInput)
  extends DDRPlacedOverlay[XilinxZCU106MIGPads](name, designInput, shellInput)
{
  val size = p(ZCU106DDRSize)

  val migParams = XilinxZCU106MIGParams(address = AddressSet.misaligned(di.baseAddress, size))
  val mig = LazyModule(new XilinxZCU106MIG(migParams))
  val ioNode = BundleBridgeSource(() => mig.module.io.cloneType)
  val topIONode = shell { ioNode.makeSink() }
  val ddrUI     = shell { ClockSourceNode(freqMHz = 300) }  // Calculated as CLKOUT0 from MIG - sys_clk * DDR4_CLKFBOUT_MULT / (DDR4_CLKOUT0_DIVIDE*DDR4_DIVCLK_DIVIDE)
  val areset    = shell { ClockSinkNode(Seq(ClockSinkParameters())) }
  areset := designInput.wrangler := ddrUI

  def overlayOutput = DDROverlayOutput(ddr = mig.node)
  def ioFactory = new XilinxZCU106MIGPads(size)

  InModuleBody { ioNode.bundle <> mig.module.io }

  shell { InModuleBody {
    require (shell.sys_clock.get.isDefined, "Use of DDRZCU106Overlay depends on SysClockZCU106Overlay")
    val (sys, _) = shell.sys_clock.get.get.overlayOutput.node.out(0)
    val (ui, _) = ddrUI.out(0)
    val (ar, _) = areset.in(0)
    val port = topIONode.bundle.port
    io <> port
    ui.clock := port.c0_ddr4_ui_clk
    ui.reset := /*!port.mmcm_locked ||*/ port.c0_ddr4_ui_clk_sync_rst
    port.c0_sys_clk_i := sys.clock.asUInt
    port.sys_rst := sys.reset // pllReset
    port.c0_ddr4_aresetn := !ar.reset

    val allddrpins = Seq(
    // adr[0->13]
    "AK9", "AG11", "AJ10", "AL8", "AK10", "AH8", "AJ9", "AG8", "AH9", "AG10", "AH13", "AG9", "AM13", "AF8",
    // we_n, cas_n, ras_n, bg0
    "AC12", "AE12", "AF11", "AE14",
    // ba[0->1]
    "AK8", "AL12",
    // reset_n, act_n, ck_c, ck_t, cke, cs_n, odt
    "AF12", "AD14", "AJ11", "AH11", "AB13", "AD12", "AF10",
    // dq[0->15]
    "AF16", "AF18", "AG15", "AF17", "AF15", "AG18", "AG14", "AE17", "AA14", "AC16", "AB15", "AD16", "AB16", "AC17", "AB14", "AD17",
    // dq[16->31]
    "AJ16", "AJ17", "AL15", "AK17", "AJ15", "AK18", "AL16", "AL18", "AP13", "AP16", "AP15", "AN16", "AN13", "AM18", "AN17", "AN18",
    // dq[32->47]
    "AB19", "AD19", "AC18", "AC19", "AA20", "AE20", "AA19", "AD20", "AF22", "AH21", "AG19", "AG21", "AE24", "AG20", "AE23", "AF21",
    // dq[48->63]
    "AL22", "AJ22", "AL23", "AJ21", "AK20", "AJ19", "AK19", "AJ20", "AP22", "AN22", "AP21", "AP23", "AM19", "AM23", "AN19", "AN23",
    // dqs_c[0->7]
    "AJ14", "AA15", "AK14", "AN14", "AB18", "AG23", "AK23", "AN21",
    // dqs_t[0->7]
    "AH14", "AA16", "AK15", "AM14", "AA18", "AF23", "AK22", "AM21",
    // dm_dbi_n[0->7]
    "AH18", "AD15", "AM16", "AP18", "AE18", "AH22", "AL20", "AP19"
    )

    (IOPin.of(io) zip allddrpins) foreach { case (io, pin) => shell.xdc.addPackagePin(io, pin) }
  } }

  shell.sdc.addGroup(pins = Seq(mig.island.module.blackbox.io.c0_ddr4_ui_clk))
}
class DDRZCU106ShellPlacer(shell: ZCU106ShellBasicOverlays, val shellInput: DDRShellInput)(implicit val valName: ValName)
  extends DDRShellPlacer[ZCU106ShellBasicOverlays] {
  def place(designInput: DDRDesignInput) = new DDRZCU106PlacedOverlay(shell, valName.name, designInput, shellInput)
}

abstract class ZCU106ShellBasicOverlays()(implicit p: Parameters) extends UltraScaleShell{
  // PLL reset causes
  val pllReset = InModuleBody { Wire(Bool()) }

  val sys_clock = Overlay(ClockInputOverlayKey, new SysClockZCU106ShellPlacer(this, ClockInputShellInput()))
  val led       = Seq.tabulate(8)(i => Overlay(LEDOverlayKey, new LEDZCU106ShellPlacer(this, LEDShellInput(color = "red", number = i))(valName = ValName(s"led_$i"))))
  val switch    = Seq.tabulate(4)(i => Overlay(SwitchOverlayKey, new SwitchZCU106ShellPlacer(this, SwitchShellInput(number = i))(valName = ValName(s"switch_$i"))))
  val button    = Seq.tabulate(5)(i => Overlay(ButtonOverlayKey, new ButtonZCU106ShellPlacer(this, ButtonShellInput(number = i))(valName = ValName(s"button_$i"))))
  val ddr       = Overlay(DDROverlayKey, new DDRZCU106ShellPlacer(this, DDRShellInput()))
}

class ZCU106Shell()(implicit p: Parameters) extends ZCU106ShellBasicOverlays
{
  // Order matters; ddr depends on sys_clock
  val uart      = Overlay(UARTOverlayKey, new UARTZCU106ShellPlacer(this, UARTShellInput()))
  val sdio      = Overlay(SPIOverlayKey, new SDIOZCU106ShellPlacer(this, SPIShellInput()))
  val jtagBScan = Overlay(JTAGDebugBScanOverlayKey, new JTAGDebugBScanZCU106ShellPlacer(this, JTAGDebugBScanShellInput()))

  val topDesign = LazyModule(p(DesignKey)(designParameters))

  // Place the sys_clock at the Shell if the user didn't ask for it
  designParameters(ClockInputOverlayKey).foreach { unused =>
    val source = unused.place(ClockInputDesignInput()).overlayOutput.node
    val sink = ClockSinkNode(Seq(ClockSinkParameters()))
    sink := source
  }

  override lazy val module = new LazyRawModuleImp(this) {
    val reset = IO(Input(Bool()))
    xdc.addPackagePin(reset, "G13")
    xdc.addIOStandard(reset, "LVCMOS18")

    val reset_ibuf = Module(new IBUF)
    reset_ibuf.io.I := reset

    val sysclk: Clock = sys_clock.get() match {
      case Some(x: SysClockZCU106PlacedOverlay) => x.clock
    }

    val powerOnReset: Bool = PowerOnResetFPGAOnly(sysclk)
    sdc.addAsyncPath(Seq(powerOnReset))

    pllReset := (reset_ibuf.io.O || powerOnReset)
  }
}

/*
   Copyright 2016 SiFive, Inc.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
