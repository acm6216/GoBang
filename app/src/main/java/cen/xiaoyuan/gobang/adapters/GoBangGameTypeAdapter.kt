package cen.xiaoyuan.gobang.adapters

import cen.xiaoyuan.gobang.data.Chess
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

class GoBangGameTypeAdapter : TypeAdapter<Chess>() {

    override fun write(out: JsonWriter, value: Chess){
        out.beginObject()
        out.name("isWhite").value(value.isWhite)
        out.name("row").value(value.row)
        out.name("col").value(value.col)
        out.endObject()
    }

    override fun read(ins: JsonReader): Chess {
        var isWhite = false
        var col = 0
        var row = 0
        ins.beginObject()
        while (ins.hasNext()){
            when(ins.nextName()){
                "id" -> ins.nextLong()
                "isWhite" -> isWhite = ins.nextBoolean()
                "col" -> col = ins.nextInt()
                else -> row = ins.nextInt()
            }
        }
        ins.endObject()
        return Chess(isWhite,col,row)
    }

}