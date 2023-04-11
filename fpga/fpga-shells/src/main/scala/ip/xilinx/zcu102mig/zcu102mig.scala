package sifive.fpgashells.ip.xilinx.zcu102mig

import Chisel._
import chisel3.experimental.{Analog,attach}
import freechips.rocketchip.util.{ElaborationArtefacts}
import freechips.rocketchip.util.GenericParameterizedBundle
import freechips.rocketchip.config._

// IP VLNV: xilinx.com:customize_ip:zcu102mig:1.0
// Black Box

class ZCU102MIGIODDR(depth : BigInt) extends GenericParameterizedBundle(depth) {
  require((depth<=0x80000000L),"ZCU102MIGIODDR supports upto 2GB depth configuraton")
  val c0_ddr4_adr           = Bits(OUTPUT,17)
  val c0_ddr4_bg            = Bits(OUTPUT,1)
  val c0_ddr4_ba            = Bits(OUTPUT,2)
  val c0_ddr4_reset_n       = Bool(OUTPUT)
  val c0_ddr4_act_n         = Bool(OUTPUT)
  val c0_ddr4_ck_c          = Bits(OUTPUT,1)
  val c0_ddr4_ck_t          = Bits(OUTPUT,1)
  val c0_ddr4_cke           = Bits(OUTPUT,1)
  val c0_ddr4_cs_n          = Bits(OUTPUT,1)
  val c0_ddr4_odt           = Bits(OUTPUT,1)

  val c0_ddr4_dq            = Analog(16.W)
  val c0_ddr4_dqs_c         = Analog(2.W)
  val c0_ddr4_dqs_t         = Analog(2.W)
  val c0_ddr4_dm_dbi_n      = Analog(2.W)
}

//reused directly in io bundle for sifive.blocks.devices.xilinxzcu102mig
trait ZCU102MIGIOClocksReset extends Bundle {
  //inputs
  //"NO_BUFFER" clock source (must be connected to IBUF outside of IP)
  val c0_sys_clk_i              = Bool(INPUT)
  //user interface signals
  val c0_ddr4_ui_clk            = Clock(OUTPUT)
  val c0_ddr4_ui_clk_sync_rst   = Bool(OUTPUT)
  val c0_ddr4_aresetn           = Bool(INPUT)
  //misc
  val c0_init_calib_complete    = Bool(OUTPUT)
  val sys_rst                   = Bool(INPUT)
}

//scalastyle:off
//turn off linter: blackbox name must match verilog module
class zcu102mig(depth : BigInt)(implicit val p:Parameters) extends BlackBox
{
  require((depth<=0x80000000L),"zcu102mig supports upto 2GB depth configuraton")

  val io = new ZCU102MIGIODDR(depth) with ZCU102MIGIOClocksReset {
    //slave interface write address ports
    val c0_ddr4_s_axi_awid            = Bits(INPUT,4)
    val c0_ddr4_s_axi_awaddr          = Bits(INPUT,28)
    val c0_ddr4_s_axi_awlen           = Bits(INPUT,8)
    val c0_ddr4_s_axi_awsize          = Bits(INPUT,3)
    val c0_ddr4_s_axi_awburst         = Bits(INPUT,2)
    val c0_ddr4_s_axi_awlock          = Bits(INPUT,1)
    val c0_ddr4_s_axi_awcache         = Bits(INPUT,4)
    val c0_ddr4_s_axi_awprot          = Bits(INPUT,3)
    val c0_ddr4_s_axi_awqos           = Bits(INPUT,4)
    val c0_ddr4_s_axi_awvalid         = Bool(INPUT)
    val c0_ddr4_s_axi_awready         = Bool(OUTPUT)
    //slave interface write data ports
    val c0_ddr4_s_axi_wdata           = Bits(INPUT,64)
    val c0_ddr4_s_axi_wstrb           = Bits(INPUT,8)
    val c0_ddr4_s_axi_wlast           = Bool(INPUT)
    val c0_ddr4_s_axi_wvalid          = Bool(INPUT)
    val c0_ddr4_s_axi_wready          = Bool(OUTPUT)
    //slave interface write response ports
    val c0_ddr4_s_axi_bready          = Bool(INPUT)
    val c0_ddr4_s_axi_bid             = Bits(OUTPUT,4)
    val c0_ddr4_s_axi_bresp           = Bits(OUTPUT,2)
    val c0_ddr4_s_axi_bvalid          = Bool(OUTPUT)
    //slave interface read address ports
    val c0_ddr4_s_axi_arid            = Bits(INPUT,4)
    val c0_ddr4_s_axi_araddr          = Bits(INPUT,28)
    val c0_ddr4_s_axi_arlen           = Bits(INPUT,8)
    val c0_ddr4_s_axi_arsize          = Bits(INPUT,3)
    val c0_ddr4_s_axi_arburst         = Bits(INPUT,2)
    val c0_ddr4_s_axi_arlock          = Bits(INPUT,1)
    val c0_ddr4_s_axi_arcache         = Bits(INPUT,4)
    val c0_ddr4_s_axi_arprot          = Bits(INPUT,3)
    val c0_ddr4_s_axi_arqos           = Bits(INPUT,4)
    val c0_ddr4_s_axi_arvalid         = Bool(INPUT)
    val c0_ddr4_s_axi_arready         = Bool(OUTPUT)
    //slave interface read data ports
    val c0_ddr4_s_axi_rready          = Bool(INPUT)
    val c0_ddr4_s_axi_rid             = Bits(OUTPUT,4)
    val c0_ddr4_s_axi_rdata           = Bits(OUTPUT,64)
    val c0_ddr4_s_axi_rresp           = Bits(OUTPUT,2)
    val c0_ddr4_s_axi_rlast           = Bool(OUTPUT)
    val c0_ddr4_s_axi_rvalid          = Bool(OUTPUT)
  }

  ElaborationArtefacts.add(
    "zcu102mig.vivado.tcl",
    """
      create_ip -vendor xilinx.com -library ip -version 2.2 -name ddr4 -module_name zcu102mig -dir $ipdir -force
      set_property -dict [list \
      CONFIG.AL_SEL                               {0} \
      CONFIG.C0.ADDR_WIDTH                        {17} \
      CONFIG.C0.BANK_GROUP_WIDTH                  {1} \
      CONFIG.C0.CKE_WIDTH                         {1} \
      CONFIG.C0.CK_WIDTH                          {1} \
      CONFIG.C0.CS_WIDTH                          {1} \
      CONFIG.C0.ControllerType                    {DDR4_SDRAM} \
      CONFIG.C0.DDR4_AUTO_AP_COL_A3               {false} \
      CONFIG.C0.DDR4_AutoPrecharge                {false} \
      CONFIG.C0.DDR4_AxiAddressWidth              {28} \
      CONFIG.C0.DDR4_AxiArbitrationScheme         {RD_PRI_REG} \
      CONFIG.C0.DDR4_AxiDataWidth                 {64} \
      CONFIG.C0.DDR4_AxiIDWidth                   {4} \
      CONFIG.C0.DDR4_AxiNarrowBurst               {false} \
      CONFIG.C0.DDR4_AxiSelection                 {true} \
      CONFIG.C0.DDR4_BurstLength                  {8} \
      CONFIG.C0.DDR4_BurstType                    {Sequential} \
      CONFIG.C0.DDR4_CLKFBOUT_MULT                {5} \
      CONFIG.C0.DDR4_CLKOUT0_DIVIDE               {5} \
      CONFIG.C0.DDR4_Capacity                     {512} \
      CONFIG.C0.DDR4_CasLatency                   {18} \
      CONFIG.C0.DDR4_CasWriteLatency              {12} \
      CONFIG.C0.DDR4_ChipSelect                   {true} \
      CONFIG.C0.DDR4_Clamshell                    {false} \
      CONFIG.C0.DDR4_CustomParts                  {no_file_loaded} \
      CONFIG.C0.DDR4_DIVCLK_DIVIDE                {1} \
      CONFIG.C0.DDR4_DataMask                     {DM_NO_DBI} \
      CONFIG.C0.DDR4_DataWidth                    {8} \
      CONFIG.C0.DDR4_Ecc                          {false} \
      CONFIG.C0.DDR4_MCS_ECC                      {false} \
      CONFIG.C0.DDR4_Mem_Add_Map                  {ROW_COLUMN_BANK} \
      CONFIG.C0.DDR4_MemoryName                   {MainMemory} \
      CONFIG.C0.DDR4_MemoryPart                   {MT40A256M16GE-075E} \
      CONFIG.C0.DDR4_MemoryType                   {Components} \
      CONFIG.C0.DDR4_MemoryVoltage                {1.2V} \
      CONFIG.C0.DDR4_OnDieTermination             {RZQ/6} \
      CONFIG.C0.DDR4_Ordering                     {Normal} \
      CONFIG.C0.DDR4_OutputDriverImpedenceControl {RZQ/7} \
      CONFIG.C0.DDR4_PhyClockRatio                {4:1} \
      CONFIG.C0.DDR4_SAVE_RESTORE                 {false} \
      CONFIG.C0.DDR4_SELF_REFRESH                 {false} \
      CONFIG.C0.DDR4_Slot                         {Single} \
      CONFIG.C0.DDR4_Specify_MandD                {true} \
      CONFIG.C0.DDR4_InputClockPeriod             {3332} \
      CONFIG.C0.DDR4_TimePeriod                   {833} \
      CONFIG.C0.DDR4_UserRefresh_ZQCS             {false} \
      CONFIG.C0.DDR4_isCKEShared                  {false} \
      CONFIG.C0.DDR4_isCustom                     {false} \
      CONFIG.C0.LR_WIDTH                          {1} \
      CONFIG.C0.ODT_WIDTH                         {1} \
      CONFIG.C0.StackHeight                       {1} \
      CONFIG.C0_CLOCK_BOARD_INTERFACE             {Custom} \
      CONFIG.C0_DDR4_BOARD_INTERFACE              {Custom} \
      CONFIG.DCI_Cascade                          {false} \
      CONFIG.DIFF_TERM_SYSCLK                     {false} \
      CONFIG.Debug_Signal                         {Disable} \
      CONFIG.Default_Bank_Selections              {false} \
      CONFIG.Enable_SysPorts                      {true} \
      CONFIG.IOPowerReduction                     {OFF} \
      CONFIG.IO_Power_Reduction                   {false} \
      CONFIG.IS_FROM_PHY                          {1} \
      CONFIG.MCS_DBG_EN                           {false} \
      CONFIG.No_Controller                        {1} \
      CONFIG.PARTIAL_RECONFIG_FLOW_MIG            {false} \
      CONFIG.PING_PONG_PHY                        {1} \
      CONFIG.Phy_Only                             {Complete_Memory_Controller} \
      CONFIG.RECONFIG_XSDB_SAVE_RESTORE           {false} \
      CONFIG.RESET_BOARD_INTERFACE                {Custom} \
      CONFIG.Reference_Clock                      {Differential} \
      CONFIG.SET_DW_TO_40                         {false} \
      CONFIG.System_Clock                         {No_Buffer} \
      CONFIG.TIMING_3DS                           {false} \
      CONFIG.TIMING_OP1                           {false} \
      CONFIG.TIMING_OP2                           {false} \
      ] [get_ips zcu102mig]"""
  )

}
//scalastyle:on

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
