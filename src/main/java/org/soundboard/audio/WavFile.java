/***
 ** 
 ** This library is free software; you can redistribute it and/or
 ** modify it under the terms of the GNU Lesser General Public
 ** License as published by the Free Software Foundation; either
 ** version 2.1 of the License, or (at your option) any later version.
 ** 
 ** This library is distributed in the hope that it will be useful,
 ** but WITHOUT ANY WARRANTY; without even the implied warranty of
 ** MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 ** Lesser General Public License for more details.
 ** 
 ** You should have received a copy of the GNU Lesser General Public
 ** License along with this library; if not, write to the Free Software
 ** Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 **
 **/
package org.soundboard.audio;


public class WavFile {

   //see http://www.sonicspot.com/guide/wavefiles.html
   
   //this is the wav file header
   //Offset   Size   Description      Value
   //------   ----   -----------      -----
   //0x00     4      Chunk ID         "RIFF" (0x52494646)
   //0x04     4      Chunk Data Size  (file size) - 8
   //0x08     4      RIFF Type        "WAVE" (0x57415645)
   byte[] header = new byte[] {
      0x52,0x49,0x46,0x46, //RIFF
      0x00,0x00,0x00,0x00, //SIZE - to be set later
      0x57,0x41,0x56,0x45  //WAVE
   };
   
   //Offset   Size   Description             Value
   //------   ----   -----------             -----
   //0x00     4      Chunk ID                "fmt " (0x666D7420)
   //0x04     4      Chunk Data Size          16 + extra format bytes
   //0x08     2      Compression code         1 - 65,535
   //0x0a     2      Number of channels       1 - 65,535
   //0x0c     4      Sample rate              1 - 0xFFFFFFFF
   //0x10     4      Average bytes/s          1 - 0xFFFFFFFF   AvgBytesPerSec = SampleRate * BlockAlign
   //0x14     2      Block align              1 - 65,535       BlockAlign = SignificantBitsPerSample / 8 * NumChannels
   //0x16     2      Significant bits/sample  2 - 65,535
   //     -- OPTIONAL --
   //0x18     2      Extra format bytes       0 - 65,535
   //0x1a          Extra format bytes *
   byte[] formatChunk = new byte[] {
      0x66,0x6D,0x74,0x20,              //fmt
      0x10,0x00,0x00,0x00,
      0x01,0x00,                        //PCM (uncompressed)
      0x01,0x00,                        //mono
      0x44,(byte)0xAC,0x00,0x00,        //
      0x00,0x00,0x00,0x00,  
      0x02,0x00,
      0x10,0x00,
   };
   
}