
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
import sifive.fpgashells.devices.xilinx.xilinxzcu102mig._
import sifive.fpgashells.devices.xilinx.xdma._
import sifive.fpgashells.ip.xilinx.xxv_ethernet._

class SysClockZCU102PlacedOverlay(val shell: ZCU102ShellBasicOverlays, name: String, val designInput: ClockInputDesignInput, val shellInput: ClockInputShellInput)
  extends LVDSClockInputXilinxPlacedOverlay(name, designInput, shellInput)
{
  val node = shell { ClockSourceNode(freqMHz = 300, jitterPS = 50)(ValName(name)) }

  shell { InModuleBody {
    shell.xdc.addPackagePin(io.p, "AL8")
    shell.xdc.addPackagePin(io.n, "AL7")
    shell.xdc.addIOStandard(io.p, "DIFF_SSTL12")
    shell.xdc.addIOStandard(io.n, "DIFF_SSTL12")
  } }
}
class SysClockZCU102ShellPlacer(shell: ZCU102ShellBasicOverlays, val shellInput: ClockInputShellInput)(implicit val valName: ValName)
  extends ClockInputShellPlacer[ZCU102ShellBasicOverlays]
{
    def place(designInput: ClockInputDesignInput) = new SysClockZCU102PlacedOverlay(shell, valName.name, designInput, shellInput)
}

class SDIOZCU102PlacedOverlay(val shell: ZCU102ShellBasicOverlays, name: String, val designInput: SPIDesignInput, val shellInput: SPIShellInput)
																			
																																				
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
	   


						
    val packagePinsWithPackageIOs = Seq(("A21", IOPin(io.spi_clk)),
                                        ("B20", IOPin(io.spi_cs)), // This is labelled as cs, but is actually cmd
                                        ("A22", IOPin(io.spi_dat(0))),
                                        ("B21", IOPin(io.spi_dat(1))),
                                        ("C21", IOPin(io.spi_dat(2))),
                                        ("A20", IOPin(io.spi_dat(3))))

    packagePinsWithPackageIOs foreach { case (pin, io) => {
      shell.xdc.addPackagePin(io, pin)
      shell.xdc.addIOStandard(io, "LVCMOS33") // There is a level translator on the ZCU102 board to convert this to 3.3V
						  
    } }
    packagePinsWithPackageIOs drop 1 foreach { case (pin, io) => {
      shell.xdc.addPullup(io)
      shell.xdc.addIOB(io)
    } }
  
  } }
}
class SDIOZCU102ShellPlacer(shell: ZCU102ShellBasicOverlays, val shellInput: SPIShellInput)(implicit val valName: ValName)
  extends SPIShellPlacer[ZCU102ShellBasicOverlays] {
  def place(designInput: SPIDesignInput) = new SDIOZCU102PlacedOverlay(shell, valName.name, designInput, shellInput)
}

class UARTZCU102PlacedOverlay(val shell: ZCU102ShellBasicOverlays, name: String, val designInput: UARTDesignInput, val shellInput: UARTShellInput)
  extends UARTXilinxPlacedOverlay(name, designInput, shellInput, true)
{
  shell { InModuleBody {
    val packagePinsWithPackageIOs = Seq(("D12", IOPin(io.ctsn.get)),
                                        ("E12", IOPin(io.rtsn.get)),
                                        ("E13", IOPin(io.rxd)),
                                        ("F13", IOPin(io.txd)))

    packagePinsWithPackageIOs foreach { case (pin, io) => {
      shell.xdc.addPackagePin(io, pin)
      shell.xdc.addIOStandard(io, "LVCMOS33") // 3.3V, part of PL DDR4 bank and there's a level translator
      shell.xdc.addIOB(io)
    } }
  } }
}
class UARTZCU102ShellPlacer(shell: ZCU102ShellBasicOverlays, val shellInput: UARTShellInput)(implicit val valName: ValName)
  extends UARTShellPlacer[ZCU102ShellBasicOverlays] {
  def place(designInput: UARTDesignInput) = new UARTZCU102PlacedOverlay(shell, valName.name, designInput, shellInput)
}

object LEDZCU102PinConstraints {
  val pins = Seq("AG14", "AF13", "AE13", "AJ14", "AJ15", "AH13", "AH14", "AL12")
 
										  
	 
}
 
class LEDZCU102PlacedOverlay(val shell: ZCU102ShellBasicOverlays, name: String, val designInput: LEDDesignInput, val shellInput: LEDShellInput)
  extends LEDXilinxPlacedOverlay(name, designInput, shellInput, packagePin = Some(LEDZCU102PinConstraints.pins(shellInput.number)), ioStandard = "LVCMOS33")
class LEDZCU102ShellPlacer(shell: ZCU102ShellBasicOverlays, val shellInput: LEDShellInput)(implicit val valName: ValName)
  extends LEDShellPlacer[ZCU102ShellBasicOverlays] {
  def place(designInput: LEDDesignInput) = new LEDZCU102PlacedOverlay(shell, valName.name, designInput, shellInput)
}

object ButtonZCU102PinConstraints {
  val pins = Seq("AG15", "AE14", "AF15", "AE15", "AG13")
}
class ButtonZCU102PlacedOverlay(val shell: ZCU102ShellBasicOverlays, name: String, val designInput: ButtonDesignInput, val shellInput: ButtonShellInput)
  extends ButtonXilinxPlacedOverlay(name, designInput, shellInput, packagePin = Some(ButtonZCU102PinConstraints.pins(shellInput.number)), ioStandard = "LVCMOS33")
class ButtonZCU102ShellPlacer(shell: ZCU102ShellBasicOverlays, val shellInput: ButtonShellInput)(implicit val valName: ValName)
  extends ButtonShellPlacer[ZCU102ShellBasicOverlays] {
  def place(designInput: ButtonDesignInput) = new ButtonZCU102PlacedOverlay(shell, valName.name, designInput, shellInput)
}

object SwitchZCU102PinConstraints {
  val pins = Seq("AN14", "AP14", "AM14", "AN13")
}
class SwitchZCU102PlacedOverlay(val shell: ZCU102ShellBasicOverlays, name: String, val designInput: SwitchDesignInput, val shellInput: SwitchShellInput)
  extends SwitchXilinxPlacedOverlay(name, designInput, shellInput, packagePin = Some(SwitchZCU102PinConstraints.pins(shellInput.number)), ioStandard = "LVCMOS33")
class SwitchZCU102ShellPlacer(shell: ZCU102ShellBasicOverlays, val shellInput: SwitchShellInput)(implicit val valName: ValName)
  extends SwitchShellPlacer[ZCU102ShellBasicOverlays] {
  def place(designInput: SwitchDesignInput) = new SwitchZCU102PlacedOverlay(shell, valName.name, designInput, shellInput)
}



class JTAGDebugBScanZCU102PlacedOverlay(val shell: ZCU102ShellBasicOverlays, name: String, val designInput: JTAGDebugBScanDesignInput, val shellInput: JTAGDebugBScanShellInput)
  extends JTAGDebugBScanXilinxPlacedOverlay(name, designInput, shellInput)
class JTAGDebugBScanZCU102ShellPlacer(val shell: ZCU102ShellBasicOverlays, val shellInput: JTAGDebugBScanShellInput)(implicit val valName: ValName)
  extends JTAGDebugBScanShellPlacer[ZCU102ShellBasicOverlays] {
  def place(designInput: JTAGDebugBScanDesignInput) = new JTAGDebugBScanZCU102PlacedOverlay(shell, valName.name, designInput, shellInput)
}

case object ZCU102DDRSize extends Field[BigInt](0x40000000L * 2) // 2GB
class DDRZCU102PlacedOverlay(val shell: ZCU102ShellBasicOverlays, name: String, val designInput: DDRDesignInput, val shellInput: DDRShellInput)
  extends DDRPlacedOverlay[XilinxZCU102MIGPads](name, designInput, shellInput)
{
  val size = p(ZCU102DDRSize)

  val migParams = XilinxZCU102MIGParams(address = AddressSet.misaligned(di.baseAddress, size))
  val mig = LazyModule(new XilinxZCU102MIG(migParams))
  val ioNode = BundleBridgeSource(() => mig.module.io.cloneType)
  val topIONode = shell { ioNode.makeSink() }
  val ddrUI     = shell { ClockSourceNode(freqMHz = 300) }  // Calculated as CLKOUT0 from MIG - sys_clk * DDR4_CLKFBOUT_MULT / (DDR4_CLKOUT0_DIVIDE*DDR4_DIVCLK_DIVIDE)
  val areset    = shell { ClockSinkNode(Seq(ClockSinkParameters())) }
  areset := designInput.wrangler := ddrUI

  def overlayOutput = DDROverlayOutput(ddr = mig.node)
  def ioFactory = new XilinxZCU102MIGPads(size)

  InModuleBody { ioNode.bundle <> mig.module.io }

  shell { InModuleBody {
    require (shell.sys_clock.get.isDefined, "Use of DDRZCU102Overlay depends on SysClockZCU102Overlay")
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
val allddrpins1 = Seq(

      "AM8", "AM9", "AP8", "AN8", "AK10", "AJ10", "AP9", "AN9", "AP10", "AP11", "AM10", "AL10", "AM11", "AL11",  // adr[0->13]
      "AJ7", "AL5", "AJ9", "AK7", // we_n, cas_n, ras_n, bg
      "AK12", "AJ12", // ba[0->1]
      "AH9", "AK8", "AP7", "AN7", "AM3", "AP2", "AK9", // reset_n, act_n, ck_c, ck_t, cke, cs_n, odt
	  
     // "AK4", "AK5", "AN4", "AM4", "AP4", "AP5", "AM5", "AM6", "AK2", "AK3", "AL1", "AK1", "AN1", "AM1", "AP3", "AN3", // dq[0->15]
     //   "AP6", "AL2", // dqs_c[0->1]
     // "AN6", "AL3", // dqs_t[0->1]
     // "AL6", "AN2") // dm_dbi_n[0->1]

//	val allddrpins2 = Seq( 

      "AK4", "AK5", "AN4", "AM4", "AP4", "AP5", "AM5", "AM6", "AK2", "AK3", "AL1", "AK1", "AN1", "AM1", "AP3", "AN3", // dq[0->15]
      "AP6", "AL2",  // dqs_c[0->1]
      "AN6", "AL3",  // dqs_t[0->1]
      "AL6", "AN2")  // dm_dbi_n[0->1]

(IOPin.of(io) zip allddrpins1) foreach { case (io, pin) => shell.xdc.addPackagePin(io, pin) }

/*
   println("*********************print start") 
	(IOPin.of(io) zip allddrpins1) foreach { case (io, pin) => {
	shell.xdc.addPackagePin(io, pin)
    shell.xdc.addIOStandard(io, "SSTL12_DCI")
println(IOPin) } }
	
   (IOPin.of(io) zip allddrpins2) foreach { case (io, pin) => {
	shell.xdc.addPackagePin(io, pin)
    shell.xdc.addIOStandard(io, "POD12_DCI" ) } }
*/

/*
Val x = "AK4" 
Val s =x match {
    case "AK4" <= (IOPin.of(io) zip allddrpins2) foreach { case (io, pin) => {
	               shell.xdc.addPackagePin(io, pin)
                    shell.xdc.addIOStandard(io, "POD12_DCI" ) 
	
  } }
   
}
*/

	
  } }
      shell.sdc.addGroup(pins = Seq(mig.island.module.blackbox.io.c0_ddr4_ui_clk))
}
class DDRZCU102ShellPlacer(shell: ZCU102ShellBasicOverlays, val shellInput: DDRShellInput)(implicit val valName: ValName)
  extends DDRShellPlacer[ZCU102ShellBasicOverlays] {
  def place(designInput: DDRDesignInput) = new DDRZCU102PlacedOverlay(shell, valName.name, designInput, shellInput)
}

																														 
 

abstract class ZCU102ShellBasicOverlays()(implicit p: Parameters) extends UltraScaleShell{
  // PLL reset causes
  val pllReset = InModuleBody { Wire(Bool()) }

  val sys_clock = Overlay(ClockInputOverlayKey, new SysClockZCU102ShellPlacer(this, ClockInputShellInput()))
																											
  val led       = Seq.tabulate(8)(i => Overlay(LEDOverlayKey, new LEDZCU102ShellPlacer(this, LEDShellInput(color = "red", number = i))(valName = ValName(s"led_$i"))))
  val switch    = Seq.tabulate(4)(i => Overlay(SwitchOverlayKey, new SwitchZCU102ShellPlacer(this, SwitchShellInput(number = i))(valName = ValName(s"switch_$i"))))
  val button    = Seq.tabulate(5)(i => Overlay(ButtonOverlayKey, new ButtonZCU102ShellPlacer(this, ButtonShellInput(number = i))(valName = ValName(s"button_$i"))))
  val ddr       = Overlay(DDROverlayKey, new DDRZCU102ShellPlacer(this, DDRShellInput()))
																									 																									 																																																  
							
}


class ZCU102Shell()(implicit p: Parameters) extends ZCU102ShellBasicOverlays
{
												  

  // Order matters; ddr depends on sys_clock
  val uart      = Overlay(UARTOverlayKey, new UARTZCU102ShellPlacer(this, UARTShellInput()))
  val sdio      = Overlay(SPIOverlayKey, new SDIOZCU102ShellPlacer(this, SPIShellInput()))
																																   
																											  
  val jtagBScan = Overlay(JTAGDebugBScanOverlayKey, new JTAGDebugBScanZCU102ShellPlacer(this, JTAGDebugBScanShellInput()))
																							   
																								

  val topDesign = LazyModule(p(DesignKey)(designParameters))

  // Place the sys_clock at the Shell if the user didn't ask for it
  designParameters(ClockInputOverlayKey).foreach { unused =>
    val source = unused.place(ClockInputDesignInput()).overlayOutput.node
    val sink = ClockSinkNode(Seq(ClockSinkParameters()))
    sink := source
  }

  override lazy val module = new LazyRawModuleImp(this) {
    val reset = IO(Input(Bool()))
    xdc.addPackagePin(reset, "AM13")
    xdc.addIOStandard(reset, "LVCMOS33")

    val reset_ibuf = Module(new IBUF)
    reset_ibuf.io.I := reset

    val sysclk: Clock = sys_clock.get() match {
      case Some(x: SysClockZCU102PlacedOverlay) => x.clock
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
