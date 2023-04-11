package sifive.fpgashells.clocks

import Chisel._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.util._

class ClockBundle(params: ClockBundleParameters) extends GenericParameterizedBundle(params)
{
  val clock = Clock()
  val reset = Bool()
}

class ClockGroupBundle(params: ClockGroupBundleParameters) extends GenericParameterizedBundle(params)
{
  val member = HeterogeneousBag(params.members.map(p => new ClockBundle(p)))
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
