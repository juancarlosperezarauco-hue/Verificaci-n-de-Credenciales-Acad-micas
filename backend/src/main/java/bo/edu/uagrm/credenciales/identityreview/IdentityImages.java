package bo.edu.uagrm.credenciales.identityreview;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.*;
import java.security.*;
import java.util.*;
import javax.imageio.*;
import javax.imageio.stream.ImageInputStream;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/** Decode only bounded raster images and discard metadata before persistence. */
@Component
public class IdentityImages {
    public static final long MAX_BYTES=5L*1024*1024;
    public byte[] normalize(MultipartFile file) {
        if(file==null||file.isEmpty()||file.getSize()>MAX_BYTES)
            throw new IllegalArgumentException("Selecciona una imagen JPEG o PNG de hasta 5 MiB por archivo.");
        try(ImageInputStream stream=ImageIO.createImageInputStream(file.getInputStream())) {
            Iterator<ImageReader> readers=ImageIO.getImageReaders(stream);
            if(!readers.hasNext())throw new IllegalArgumentException("El archivo no es una imagen JPEG o PNG válida.");
            ImageReader reader=readers.next();
            try {
                String format=reader.getFormatName().toLowerCase(Locale.ROOT);
                if(!Set.of("jpeg","jpg","png").contains(format))throw new IllegalArgumentException("Solo se admiten imágenes JPEG y PNG.");
                reader.setInput(stream,true,true);
                int width=reader.getWidth(0),height=reader.getHeight(0);
                if(width<1||height<1||width>6000||height>6000||(long)width*height>12_000_000)
                    throw new IllegalArgumentException("La imagen debe tener hasta 12 megapíxeles y 6000 píxeles por lado.");
                BufferedImage source=reader.read(0),clean=new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);
                var graphics=clean.createGraphics();
                try {graphics.setColor(Color.WHITE);graphics.fillRect(0,0,width,height);graphics.drawImage(source,0,0,null);}
                finally {graphics.dispose();source.flush();}
                var result=new ByteArrayOutputStream();
                try {if(!ImageIO.write(clean,"jpeg",result))throw new IOException();return result.toByteArray();}
                finally {clean.flush();}
            } finally {reader.dispose();}
        } catch(IOException failure) {throw new IllegalArgumentException("No se pudo leer la imagen. Usa un archivo JPEG o PNG válido.");}
    }
    public static String hash(byte[] bytes) {
        try {return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}
        catch(NoSuchAlgorithmException failure){throw new IllegalStateException("SHA-256 no disponible");}
    }
}
