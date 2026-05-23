package com.xcelerate;

import com.sun.jna.Callback;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

interface ExcelLib extends Library {

    ExcelLib INSTANCE = Native.load(NativeLoader.resolve(), ExcelLib.class);

    interface WriteCallback extends Callback {
        void invoke(Pointer data, int length, Pointer ctx);
    }

    Pointer xcelerateOpen(
        String[]      headers,
        int           headerCount,
        int           fileType,   // 0=EXCEL, 1=CSV
        WriteCallback callback,
        Pointer       ctx
    );

    int xcelerateWrite(
        Pointer  session,
        String[] data,
        int      rowCount,
        int      colCount
    );

    int xcelerateClose(Pointer session);
}
