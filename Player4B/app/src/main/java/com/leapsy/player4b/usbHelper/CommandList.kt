package com.leapsy.player4b.usbHelper

class CommandList {
    companion object {
        private const val SET_BRIGHT = "setbright"
        private const val SET_EXT2PORT = "setext2port"
        private const val SET_2D3D = "set2d3d"
        val SET_LIST = arrayOf(SET_BRIGHT, SET_EXT2PORT, SET_2D3D)
        fun sendSetCommand(aCommand : String, aValue : String) : String { return "${aCommand} ${aValue}" }
    }
}