package com.galaxy.batteryhook;

import android.content.Context;
import android.graphics.Typeface;
import android.net.Uri;
import android.widget.TextView;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/** Local user-provided typeface. No font assets are bundled or redistributed. */
final class FontStore {
    static final int REQUEST=8143;
    private static final String NAME="colour_battery_oppo_sans.ttf";
    private static Typeface cached;
    private static long cachedStamp=-2;
    private static File file(Context c){return new File(c.getFilesDir(),NAME);}
    static boolean has(Context c){File f=file(c);return f.isFile()&&f.length()>1024;}
    static synchronized Typeface get(Context c){
        File f=file(c);long stamp=f.isFile()?f.lastModified():-1;
        if(cached!=null&&stamp==cachedStamp)return cached;
        Typeface t=null;
        if(f.isFile()&&f.length()>1024&&f.length()<=32000000){
            try{t=new Typeface.Builder(f).build();}catch(Throwable e){BatteryHook.log("Imported font unreadable: "+e);}
        }
        if(t==null){t=new TextView(c).getTypeface();if(t==null)t=Typeface.DEFAULT;}
        cachedStamp=stamp;cached=t;return cached;
    }
    static synchronized void clear(Context c){File f=file(c);if(f.exists())f.delete();cached=null;cachedStamp=-2;}
    static void importFromDocument(Context c,Uri uri)throws Exception{
        if(uri==null)throw new IOException("No font selected");
        File target=file(c),tmp=new File(c.getFilesDir(),NAME+".tmp");
        long count=0;
        try(InputStream in=c.getContentResolver().openInputStream(uri);OutputStream out=new FileOutputStream(tmp)){
            if(in==null)throw new IOException("File unavailable");
            byte[] buf=new byte[8192];int n;
            while((n=in.read(buf))!=-1){count+=n;if(count>32000000)throw new IOException("Font exceeds 32 MB");out.write(buf,0,n);}
        }catch(Exception e){tmp.delete();throw e;}
        try{
            if(count<1024)throw new IOException("Font file is empty");
            try(DataInputStream in=new DataInputStream(new FileInputStream(tmp))){
                int magic=in.readInt();
                if(magic!=0x00010000&&magic!=0x4f54544f&&magic!=0x74746366&&magic!=0x74727565)
                    throw new IOException("Choose a TTF or OTF file");
            }
            Typeface face=new Typeface.Builder(tmp).build();if(face==null)throw new IOException("Unable to load selected font");
            Files.move(tmp.toPath(),target.toPath(),StandardCopyOption.REPLACE_EXISTING);
            synchronized(FontStore.class){cached=face;cachedStamp=target.lastModified();}
        }finally{tmp.delete();}
    }
}