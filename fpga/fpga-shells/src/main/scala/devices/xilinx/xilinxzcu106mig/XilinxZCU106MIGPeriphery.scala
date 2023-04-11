package sifive.fpgashells.devices.xilinx.xilinxzcu106mig

import Chisel._
import freechips.rocketchip.config._
import freechips.rocketchip.subsystem.BaseSubsystem
import freechips.rocketchip.diplomacy.{LazyModule, LazyModuleImp, AddressRange}

case object MemoryXilinxDDRKey extends Field[XilinxZCU106MIGParams]

trait HasMemoryXilinxZCU106MIG { this: BaseSubsystem =>
  val module: HasMemoryXilinxZCU106MIGModuleImp

  val xilinxzcu106mig = LazyModule(new XilinxZCU106MIG(p(MemoryXilinxDDRKey)))

  xilinxzcu106mig.node := mbus.toDRAMController(Some("xilinxzcu106mig"))()
}

trait HasMemoryXilinxZCU106MIGBundle {
  val xilinxzcu106mig: XilinxZCU106MIGIO
  def connectXilinxZCU106MIGToPads(pads: XilinxZCU106MIGPads) {
    pads <> xilinxzcu106mig
  }
}

trait HasMemoryXilinxZCU106MIGModuleImp extends LazyModuleImp
    with HasMemoryXilinxZCU106MIGBundle {
  val outer: HasMemoryXilinxZCU106MIG
  val ranges = AddressRange.fromSets(p(MemoryXilinxDDRKey).address)
  require (ranges.size == 1, "DDR range must be contiguous")
  val depth = ranges.head.size
  val xilinxzcu106mig = IO(new XilinxZCU106MIGIO(depth))

  xilinxzcu106mig <> outer.xilinxzcu106mig.module.io.port
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
