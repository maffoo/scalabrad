package org.labrad.data

import java.io.{BufferedOutputStream, FilterInputStream, InputStream, OutputStream}
import java.nio.ByteOrder
import java.nio.ByteOrder.BIG_ENDIAN

class PacketInputStream(in: InputStream)(implicit bo: ByteOrder = BIG_ENDIAN)
extends FilterInputStream(in) {
  // Read a single packet from the input stream.
  def readPacket = Packet.fromBytes(in)
}

// Output stream that writes LabRAD packets.
class PacketOutputStream(out: OutputStream)(implicit bo: ByteOrder = BIG_ENDIAN)
extends BufferedOutputStream(out) {
  // Write a packet to the output stream.
  def writePacket(packet: Packet): Unit = {
    out.write(packet.toBytes)
    out.flush()
  }
}
