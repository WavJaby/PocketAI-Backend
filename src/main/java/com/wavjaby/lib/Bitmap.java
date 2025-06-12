package com.wavjaby.lib;

public class Bitmap {
    public final int width, height;
    public final byte[] pixels;

    public Bitmap(int width, int height, byte[] pixels) {
        this.width = width;
        this.height = height;
        this.pixels = pixels;
    }
}
