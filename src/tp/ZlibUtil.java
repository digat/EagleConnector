/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package tp;

/**
 *
 * @author Tareq
 */
import com.jcraft.jzlib.Deflater;
import com.jcraft.jzlib.Inflater;
import com.jcraft.jzlib.JZlib;
import io.netty.handler.codec.compression.CompressionException;
import io.netty.handler.codec.compression.ZlibWrapper;

/**
 * Utility methods used by {@link JZlibEncoder} and {@link JZlibDecoder}.
 */
final class ZlibUtil {

    static void fail(Inflater z, String message, int resultCode) {
        throw inflaterException(z, message, resultCode);
    }

    static void fail(Deflater z, String message, int resultCode) {
        throw deflaterException(z, message, resultCode);
    }

    static CompressionException inflaterException(Inflater z, String message, int resultCode) {
        return new CompressionException(message + " (" + resultCode + ')' + (z.msg != null? ": " + z.msg : ""));
    }

    static CompressionException deflaterException(Deflater z, String message, int resultCode) {
        return new CompressionException(message + " (" + resultCode + ')' + (z.msg != null? ": " + z.msg : ""));
    }

    static JZlib.WrapperType convertWrapperType(ZlibWrapper wrapper) {
        JZlib.WrapperType convertedWrapperType;
        switch (wrapper) {
        case NONE:
            convertedWrapperType = JZlib.W_NONE;
            break;
        case ZLIB:
            convertedWrapperType = JZlib.W_ZLIB;
            break;
        case GZIP:
            convertedWrapperType = JZlib.W_GZIP;
            break;
        case ZLIB_OR_NONE:
            convertedWrapperType = JZlib.W_ANY;
            break;
        default:
            throw new Error();
        }
        return convertedWrapperType;
    }

    private ZlibUtil() {
    }
}

