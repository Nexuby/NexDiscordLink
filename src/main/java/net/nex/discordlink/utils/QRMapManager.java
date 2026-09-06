package net.nex.discordlink.utils;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.map.MinecraftFont;

import java.awt.image.BufferedImage;

public class QRMapManager {

    public static ItemStack createQRMap(Player player, String data) {
        MapView view = Bukkit.createMap(player.getWorld());
        view.getRenderers().clear();
        view.addRenderer(new QRRenderer(data));

        ItemStack map = new ItemStack(Material.FILLED_MAP);
        MapMeta meta = (MapMeta) map.getItemMeta();
        meta.setMapView(view);
        meta.setDisplayName("§a2FA QR Code");
        map.setItemMeta(meta);

        return map;
    }

    private static class QRRenderer extends MapRenderer {
        private final String data;
        private BufferedImage image;
        private boolean rendered = false;

        public QRRenderer(String data) {
            this.data = data;
        }

        @Override
        public void render(MapView map, MapCanvas canvas, Player player) {
            if (rendered) return;

            if (image == null) {
                try {
                    QRCodeWriter writer = new QRCodeWriter();
                    BitMatrix matrix = writer.encode(data, BarcodeFormat.QR_CODE, 128, 128);
                    image = MatrixToImageWriter.toBufferedImage(matrix);
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
            }

            // Draw the image centered (128x128 on a 128x128 map)
            canvas.drawImage(0, 0, image);
            canvas.drawText(5, 5, MinecraftFont.Font, "Scan with App");
            rendered = true;
        }
    }
}
