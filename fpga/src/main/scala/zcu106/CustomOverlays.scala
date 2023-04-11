package chipyard.fpga.zcu106

import chisel3._

import freechips.rocketchip.diplomacy._
import freechips.rocketchip.config.{Parameters, Field}
import freechips.rocketchip.tilelink.{TLInwardNode, TLAsyncCrossingSink}

import sifive.fpgashells.shell._
import sifive.fpgashells.ip.xilinx._
import sifive.fpgashells.shell.xilinx._
import sifive.fpgashells.clocks._
import sifive.fpgashells.devices.xilinx.xilinxzcu106mig.{XilinxZCU106MIGPads, XilinxZCU106MIGParams, XilinxZCU106MIG}

class SysClock2ZCU106PlacedOverlay(val shell: ZCU106ShellBasicOverlays, name: String, val designInput: ClockInputDesignInput, val shellInput: ClockInputShellInput)
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
class SysClock2ZCU106ShellPlacer(shell: ZCU106ShellBasicOverlays, val shellInput: ClockInputShellInput)(implicit val valName: ValName)
  extends ClockInputShellPlacer[ZCU106ShellBasicOverlays]
{
    def place(designInput: ClockInputDesignInput) = new SysClock2ZCU106PlacedOverlay(shell, valName.name, designInput, shellInput)
}

case object ZCU106DDR2Size extends Field[BigInt](0x40000000L * 2) // 2GB
class DDR2ZCU106PlacedOverlay(val shell: ZCU106FPGATestHarness, name: String, val designInput: DDRDesignInput, val shellInput: DDRShellInput)
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

  // since this uses a separate clk/rst need to put an async crossing
  val asyncSink = LazyModule(new TLAsyncCrossingSink())
  val migClkRstNode = BundleBridgeSource(() => new Bundle {
    val clock = Output(Clock())
    val reset = Output(Bool())
  })
  val topMigClkRstIONode = shell { migClkRstNode.makeSink() }

  def overlayOutput = DDROverlayOutput(ddr = mig.node)
  def ioFactory = new XilinxZCU106MIGPads(size)

  InModuleBody {
    ioNode.bundle <> mig.module.io

    // setup async crossing
    asyncSink.module.clock := migClkRstNode.bundle.clock
    asyncSink.module.reset := migClkRstNode.bundle.reset
  }

  shell { InModuleBody {
    require (shell.sys_clock2.get.isDefined, "Use of DDRZCU106Overlay depends on SysClock2ZCU106Overlay")
    val (sys, _) = shell.sys_clock2.get.get.overlayOutput.node.out(0)
    val (ui, _) = ddrUI.out(0)
    val (ar, _) = areset.in(0)

    // connect the async fifo sync to sys_clock2
    topMigClkRstIONode.bundle.clock := sys.clock
    topMigClkRstIONode.bundle.reset := sys.reset

    val port = topIONode.bundle.port
    io <> port
    ui.clock := port.c0_ddr4_ui_clk
    ui.reset := /*!port.mmcm_locked ||*/ port.c0_ddr4_ui_clk_sync_rst
    port.c0_sys_clk_i := sys.clock.asUInt
    port.sys_rst := sys.reset // pllReset
    port.c0_ddr4_aresetn := !ar.reset

    // This was just copied from the SiFive example, but it's hard to follow.
    // The pins are emitted in the following order:
    // adr[0->13], we_n, cas_n, ras_n, bg, ba[0->1], reset_n, act_n, ck_c, ck_t, cke, cs_n, odt, dq[0->63], dqs_c[0->7], dqs_t[0->7], dm_dbi_n[0->7]
    val allddrpins = Seq(
      "AK9", "AG11", "AJ10", "AL8", "AK10", "AH8", "AJ9", "AG8", "AH9", "AG10", "AH13", "AG9", "AM13", "AF8", // adr[0->13]
      "AC12", "AE12", "AF11", "AE14", // we_n, cas_n, ras_n, bg
      "AK8", "AL12", // ba[0->1]
      "AF12", "AD14", "AJ11", "AH11", "AB13", "AD12", "AF10", // reset_n, act_n, ck_c, ck_t, cke, cs_n, odt
      "AF16", "AF18", "AG15", "AF17", "AF15", "AG18", "AG14", "AE17", "AA14", "AC16", "AB15", "AD16", "AB16", "AC17", "AB14", "AD17", // dq[0->15]
      "AJ16", "AJ17", "AL15", "AK17", "AJ15", "AK18", "AL16", "AL18", "AP13", "AP16", "AP15", "AN16", "AN13", "AM18", "AN17", "AN18", // dq[16->31]
      "AB19", "AD19", "AC18", "AC19", "AA20", "AE20", "AA19", "AD20", "AF22", "AH21", "AG19", "AG21", "AE24", "AG20", "AE23", "AF21", // dq[32->47]
      "AL22", "AJ22", "AL23", "AJ21", "AK20", "AJ19", "AK19", "AJ20", "AP22", "AN22", "AP21", "AP23", "AM19", "AM23", "AN19", "AN23", // dq[48->63]
      "AJ14", "AA15", "AK14", "AN14", "AB18", "AG23", "AK23", "AN21", // dqs_c[0->7]
      "AH14", "AA16", "AK15", "AM14", "AA18", "AF23", "AK22", "AM21", // dqs_t[0->7]
      "AH18", "AD15", "AM16", "AP18", "AE18", "AH22", "AL20", "AP19") // dm_dbi_n[0->7]

    (IOPin.of(io) zip allddrpins) foreach { case (io, pin) => shell.xdc.addPackagePin(io, pin) }
  } }

  shell.sdc.addGroup(pins = Seq(mig.island.module.blackbox.io.c0_ddr4_ui_clk))
}

class DDR2ZCU106ShellPlacer(shell: ZCU106FPGATestHarness, val shellInput: DDRShellInput)(implicit val valName: ValName)
  extends DDRShellPlacer[ZCU106FPGATestHarness] {
  def place(designInput: DDRDesignInput) = new DDR2ZCU106PlacedOverlay(shell, valName.name, designInput, shellInput)
}
