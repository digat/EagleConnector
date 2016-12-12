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


import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.compression.ZlibEncoder;
import io.netty.handler.codec.compression.ZlibWrapper;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.CRC32;
import java.util.zip.Deflater;


/**
 * Compresses a {@link ByteBuf} using the deflate algorithm.
 */
public class MyJZLibEncoder extends ZlibEncoder {

    //private final byte[] encodeBuf = new byte[8192*4];
    private final byte[] encodeBuf = new byte[8192*8]; //64
    private final Deflater deflater;
    private final AtomicBoolean finished = new AtomicBoolean();
    private volatile ChannelHandlerContext ctx;

    /*
     * GZIP support
     */
    private final boolean gzip;
    private final CRC32 crc = new CRC32();
    private static final byte[] gzipHeader = {0x1f, (byte) 0x8b, Deflater.DEFLATED, 0, 0, 0, 0, 0, 0, 0};
    private boolean writeHeader = true;

    /**
     * Creates a new zlib encoder with the default compression level ({@code 6})
     * and the default wrapper ({@link ZlibWrapper#ZLIB}).
     *
     */
    public MyJZLibEncoder() {
        this(6);
    }

    /**
     * Creates a new zlib encoder with the specified {@code compressionLevel}
     * and the default wrapper ({@link ZlibWrapper#ZLIB}).
     *
     * @param compressionLevel
     *        {@code 1} yields the fastest compression and {@code 9} yields the
     *        best compression.  {@code 0} means no compression.  The default
     *        compression level is {@code 6}.
     */
    public MyJZLibEncoder(int compressionLevel) {
        this(ZlibWrapper.ZLIB, compressionLevel);
    }

    /**
     * Creates a new zlib encoder with the default compression level ({@code 6})
     * and the specified wrapper.
     *
     * @param wrapper
     */
    public MyJZLibEncoder(ZlibWrapper wrapper) {
        this(wrapper, 6);
    }

    /**
     * Creates a new zlib encoder with the specified {@code compressionLevel}
     * and the specified wrapper.
     *
     * @param wrapper
     * @param compressionLevel
     *        {@code 1} yields the fastest compression and {@code 9} yields the
     *        best compression.  {@code 0} means no compression.  The default
     *        compression level is {@code 6}.
     */
    public MyJZLibEncoder(ZlibWrapper wrapper, int compressionLevel) {
        if (compressionLevel < 0 || compressionLevel > 9) {
            throw new IllegalArgumentException(
                    "compressionLevel: " + compressionLevel + " (expected: 0-9)");
        }
        if (wrapper == null) {
            throw new NullPointerException("wrapper");
        }
        if (wrapper == ZlibWrapper.ZLIB_OR_NONE) {
            throw new IllegalArgumentException(
                    "wrapper '" + ZlibWrapper.ZLIB_OR_NONE + "' is not " +
                    "allowed for compression.");
        }

        gzip = wrapper == ZlibWrapper.GZIP;
        deflater = new Deflater(compressionLevel, wrapper != ZlibWrapper.ZLIB);
    }

    /**
     * Creates a new zlib encoder with the default compression level ({@code 6})
     * and the specified preset dictionary.  The wrapper is always
     * {@link ZlibWrapper#ZLIB} because it is the only format that supports
     * the preset dictionary.
     *
     * @param dictionary  the preset dictionary
     */
    public MyJZLibEncoder(byte[] dictionary) {
        this(6, dictionary);
    }

    /**
     * Creates a new zlib encoder with the specified {@code compressionLevel}
     * and the specified preset dictionary.  The wrapper is always
     * {@link ZlibWrapper#ZLIB} because it is the only format that supports
     * the preset dictionary.
     *
     * @param compressionLevel
     *        {@code 1} yields the fastest compression and {@code 9} yields the
     *        best compression.  {@code 0} means no compression.  The default
     *        compression level is {@code 6}.
     * @param dictionary  the preset dictionary
     */
    public MyJZLibEncoder(int compressionLevel, byte[] dictionary) {
        if (compressionLevel < 0 || compressionLevel > 9) {
            throw new IllegalArgumentException(
                    "compressionLevel: " + compressionLevel + " (expected: 0-9)");
        }
        if (dictionary == null) {
            throw new NullPointerException("dictionary");
        }

        gzip = false;
        deflater = new Deflater(compressionLevel);
        deflater.setDictionary(dictionary);
    }

    @Override
    public ChannelFuture close() {
        return close(ctx().newPromise());
    }

    @Override
    public ChannelFuture close(ChannelPromise future) {
        return finishEncode(ctx(), future);
    }

    private ChannelHandlerContext ctx() {
        ChannelHandlerContext ctxlocal = this.ctx;
        if (ctxlocal == null) {
            throw new IllegalStateException("not added to a pipeline");
        }
        return ctxlocal;
    }

    @Override
    public boolean isClosed() {
        return finished.get();
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf in, ByteBuf out) throws Exception {
        if (finished.get()) {
            out.writeBytes(in);
            return;
        }

        ByteBuf uncompressed = in;
        byte[] inAry = new byte[uncompressed.readableBytes()];
        uncompressed.readBytes(inAry); 

        int sizeEstimate = (int) Math.ceil(inAry.length * 1.001) + 12;
        out.ensureWritable(sizeEstimate);

        synchronized (deflater) {
            if (gzip) {
                crc.update(inAry);
                if (writeHeader) {
                    out.writeBytes(gzipHeader);
                    writeHeader = false;
                }
            }

            deflater.setInput(inAry);
            while (!deflater.needsInput()) {
                int numBytes = deflater.deflate(encodeBuf, 0, encodeBuf.length, Deflater.SYNC_FLUSH);
                out.writeBytes(encodeBuf, 0, numBytes);
                //System.err.println("[Send][numBytes] = "+numBytes);
            }
        }
    }

    @Override
    public void close(final ChannelHandlerContext ctx, final ChannelPromise promise) throws Exception {
        ChannelFuture f = finishEncode(ctx, ctx.newPromise());
        f.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture f) throws Exception {
                ctx.close(promise);
            }
        });

        if (!f.isDone()) {
            // Ensure the channel is closed even if the write operation completes in time.
            ctx.executor().schedule(new Runnable() {
                @Override
                public void run() {
                    ctx.close(promise);
                }
            }, 10, TimeUnit.SECONDS); // FIXME: Magic number
        }
    }

    private ChannelFuture finishEncode(final ChannelHandlerContext ctx, ChannelPromise promise) {
        if (!finished.compareAndSet(false, true)) {
            promise.setSuccess();
            return promise;
        }

        ByteBuf footer = ctx.alloc().buffer();
        synchronized (deflater) {
            deflater.finish();
            while (!deflater.finished()) {
                int numBytes = deflater.deflate(encodeBuf, 0, encodeBuf.length);
                footer.writeBytes(encodeBuf, 0, numBytes);
            }
            if (gzip) {
                int crcValue = (int) crc.getValue();
                int uncBytes = deflater.getTotalIn();
                footer.writeByte(crcValue);
                footer.writeByte(crcValue >>> 8);
                footer.writeByte(crcValue >>> 16);
                footer.writeByte(crcValue >>> 24);
                footer.writeByte(uncBytes);
                footer.writeByte(uncBytes >>> 8);
                footer.writeByte(uncBytes >>> 16);
                footer.writeByte(uncBytes >>> 24);
            }
            deflater.end();
        }

        return ctx.writeAndFlush(footer, promise);
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        this.ctx = ctx;
    }
}