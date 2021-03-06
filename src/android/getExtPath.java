package cordova.plugins.getExtPath;

import android.app.Activity;
import android.content.ContentResolver;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.Object;
import java.lang.Throwable;
import java.lang.Exception;
import java.io.IOException;
import java.io.FileNotFoundException;

import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Environment;

import org.apache.cordova.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import java.lang.Override;
import java.lang.String;
import java.util.ArrayList;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.provider.MediaStore;
import android.util.Base64;
import android.content.Context;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build.VERSION;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.util.Log;

public class getExtPath extends CordovaPlugin {

    private ArrayList<HashMap<String, String>> songsList = new  ArrayList<HashMap<String, String>>();

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("coolMethod")) {
            String message = args.getString(0);
            this.getAllSongs(callbackContext);
            return true;
        }
        return false;
    }

    private void getAllSongs(CallbackContext callbackContext){
        ContentResolver musicResolver = this.cordova.getActivity().getContentResolver();
        Uri musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor musicCursor = musicResolver.query(musicUri, null, null, null, null);

        if (musicCursor != null && musicCursor.moveToFirst()) {

            JSONArray jsonArray = new JSONArray();
            MediaMetadataRetriever mmr = new MediaMetadataRetriever();
            JSONObject items = null;

            //variables para cuando no existen metadatos
            int titleColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.TITLE);
            int idColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media._ID);
            int artistColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.ARTIST);

            try {


                do {
                    items = new JSONObject();
                    byte[] art;

                    //sin metadatos
                    long thisId = musicCursor.getLong(idColumn);
                    String thisPath = musicCursor.getString(musicCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
                    //String thisName = musicCursor.getString(musicCursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME));
                    String thisTitle = musicCursor.getString(titleColumn);
                    String thisArtist = musicCursor.getString(artistColumn);
                    String Duration = musicCursor.getString(musicCursor.getColumnIndex(MediaStore.Audio.Media.DURATION));

                    mmr.setDataSource(thisPath); //para obtener los metadatos

                    //con metadatos
                    String album = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
                    String author = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
                    String title = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
                    String genero = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE);
                    String encoded = "";
                    String blurred = "";
                    String encodedImage = "";
                    String blurImage = "";
                    art = mmr.getEmbeddedPicture();

                    if(album == null){
                        album = thisArtist;
                    }

                    if(author == null){
                        author = thisArtist;
                    }

                    if(title == null){
                        title = thisTitle;
                    }


                    // convert the byte array to a bitmap
                    if(art != null) {

                        Bitmap songImage = BitmapFactory.decodeByteArray(art, 0, art.length);
                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                        songImage.compress(Bitmap.CompressFormat.JPEG, 60, byteArrayOutputStream);
                        byte[] byteArray = byteArrayOutputStream.toByteArray();
                        encodedImage = Base64.encodeToString(byteArray, Base64.DEFAULT);
                        //guardar archivo en el sistema
                        String pathtoImg = "";
                        byte[] imageByte = Base64.decode(encodedImage, Base64.DEFAULT);
                        try {
                            pathtoImg = Environment.getExternalStorageDirectory()+"/"+thisId+".jpg";
                            File filePath = new File(pathtoImg);
                            FileOutputStream fos = new FileOutputStream(filePath, true);
                            encoded = pathtoImg;
                            fos.write(imageByte);
                            fos.flush();
                            fos.close();
                        } catch(FileNotFoundException fnfe) { 
                            callbackContext.error(fnfe.getMessage());
                        } catch(IOException ioe){
                            callbackContext.error(ioe.getMessage());
                        }

                        //filtro gausiano
                        Blur blur = new Blur();
                        Context context = this.cordova.getActivity().getApplicationContext(); 
                        Bitmap blurimg = blur.fastblur(context,songImage,20);

                        ByteArrayOutputStream byteArrayOutputStreams = new ByteArrayOutputStream();
                        
                        blurimg.compress(Bitmap.CompressFormat.JPEG, 60, byteArrayOutputStreams);
                        byte[] byteArrays = byteArrayOutputStreams.toByteArray();
                        blurImage = Base64.encodeToString(byteArrays, Base64.DEFAULT);
                       
                        //guardar archivo en el sistema
                        byte[] imageBytes = Base64.decode(blurImage, Base64.DEFAULT);
                        try {
                            pathtoImg = Environment.getExternalStorageDirectory()+"/"+thisId+"-blur.jpg";
                            File filePath = new File(pathtoImg);
                            FileOutputStream fos = new FileOutputStream(filePath, true);
                            blurred = pathtoImg;
                            fos.write(imageBytes);
                            fos.flush();
                            fos.close();
                        } catch(FileNotFoundException fnfe) { 
                            callbackContext.error(fnfe.getMessage());
                        } catch(IOException ioe){
                            callbackContext.error(ioe.getMessage());
                        }
                        
                    }


                    items.put("Id", thisId);
                    items.put("Album", album);
                    items.put("Author", author);
                    items.put("Title", title);
                    items.put("Genre", genero);
                    items.put("Cover", encoded);
                    items.put("Blur", blurred);
                    items.put("Duration", Duration);
                    items.put("Path", thisPath);
                    jsonArray.put(items);

                } while (musicCursor.moveToNext());
                callbackContext.success(jsonArray);
                mmr.release();

            } catch (RuntimeException e) {
                callbackContext.error(e.toString());
                mmr.release();
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                callbackContext.error("JSON Error! "+e.getMessage());
                mmr.release();
            }
        }
    }

}

class Blur {

  private static final String TAG = "Blur";

  @SuppressLint("NewApi")
  public static Bitmap fastblur(Context context, Bitmap sentBitmap, int radius) {

    if (VERSION.SDK_INT > 16) {
      Bitmap bitmap = sentBitmap.copy(sentBitmap.getConfig(), true);

      final RenderScript rs = RenderScript.create(context);
      final Allocation input = Allocation.createFromBitmap(rs, sentBitmap, Allocation.MipmapControl.MIPMAP_NONE,
          Allocation.USAGE_SCRIPT);
      final Allocation output = Allocation.createTyped(rs, input.getType());
      final ScriptIntrinsicBlur script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
      script.setRadius(radius);
      script.setInput(input);
      script.forEach(output);
      output.copyTo(bitmap);
      return bitmap;
    }

    Bitmap bitmap = sentBitmap.copy(sentBitmap.getConfig(), true);

    if (radius < 1) {
      return (null);
    }

    int w = bitmap.getWidth();
    int h = bitmap.getHeight();

    int[] pix = new int[w * h];
    Log.e("pix", w + " " + h + " " + pix.length);
    bitmap.getPixels(pix, 0, w, 0, 0, w, h);

    int wm = w - 1;
    int hm = h - 1;
    int wh = w * h;
    int div = radius + radius + 1;

    int r[] = new int[wh];
    int g[] = new int[wh];
    int b[] = new int[wh];
    int rsum, gsum, bsum, x, y, i, p, yp, yi, yw;
    int vmin[] = new int[Math.max(w, h)];

    int divsum = (div + 1) >> 1;
    divsum *= divsum;
    int dv[] = new int[256 * divsum];
    for (i = 0; i < 256 * divsum; i++) {
      dv[i] = (i / divsum);
    }

    yw = yi = 0;

    int[][] stack = new int[div][3];
    int stackpointer;
    int stackstart;
    int[] sir;
    int rbs;
    int r1 = radius + 1;
    int routsum, goutsum, boutsum;
    int rinsum, ginsum, binsum;

    for (y = 0; y < h; y++) {
      rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
      for (i = -radius; i <= radius; i++) {
        p = pix[yi + Math.min(wm, Math.max(i, 0))];
        sir = stack[i + radius];
        sir[0] = (p & 0xff0000) >> 16;
        sir[1] = (p & 0x00ff00) >> 8;
        sir[2] = (p & 0x0000ff);
        rbs = r1 - Math.abs(i);
        rsum += sir[0] * rbs;
        gsum += sir[1] * rbs;
        bsum += sir[2] * rbs;
        if (i > 0) {
          rinsum += sir[0];
          ginsum += sir[1];
          binsum += sir[2];
        } else {
          routsum += sir[0];
          goutsum += sir[1];
          boutsum += sir[2];
        }
      }
      stackpointer = radius;

      for (x = 0; x < w; x++) {

        r[yi] = dv[rsum];
        g[yi] = dv[gsum];
        b[yi] = dv[bsum];

        rsum -= routsum;
        gsum -= goutsum;
        bsum -= boutsum;

        stackstart = stackpointer - radius + div;
        sir = stack[stackstart % div];

        routsum -= sir[0];
        goutsum -= sir[1];
        boutsum -= sir[2];

        if (y == 0) {
          vmin[x] = Math.min(x + radius + 1, wm);
        }
        p = pix[yw + vmin[x]];

        sir[0] = (p & 0xff0000) >> 16;
        sir[1] = (p & 0x00ff00) >> 8;
        sir[2] = (p & 0x0000ff);

        rinsum += sir[0];
        ginsum += sir[1];
        binsum += sir[2];

        rsum += rinsum;
        gsum += ginsum;
        bsum += binsum;

        stackpointer = (stackpointer + 1) % div;
        sir = stack[(stackpointer) % div];

        routsum += sir[0];
        goutsum += sir[1];
        boutsum += sir[2];

        rinsum -= sir[0];
        ginsum -= sir[1];
        binsum -= sir[2];

        yi++;
      }
      yw += w;
    }
    for (x = 0; x < w; x++) {
      rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
      yp = -radius * w;
      for (i = -radius; i <= radius; i++) {
        yi = Math.max(0, yp) + x;

        sir = stack[i + radius];

        sir[0] = r[yi];
        sir[1] = g[yi];
        sir[2] = b[yi];

        rbs = r1 - Math.abs(i);

        rsum += r[yi] * rbs;
        gsum += g[yi] * rbs;
        bsum += b[yi] * rbs;

        if (i > 0) {
          rinsum += sir[0];
          ginsum += sir[1];
          binsum += sir[2];
        } else {
          routsum += sir[0];
          goutsum += sir[1];
          boutsum += sir[2];
        }

        if (i < hm) {
          yp += w;
        }
      }
      yi = x;
      stackpointer = radius;
      for (y = 0; y < h; y++) {
        // Preserve alpha channel: ( 0xff000000 & pix[yi] )
        pix[yi] = (0xff000000 & pix[yi]) | (dv[rsum] << 16) | (dv[gsum] << 8) | dv[bsum];

        rsum -= routsum;
        gsum -= goutsum;
        bsum -= boutsum;

        stackstart = stackpointer - radius + div;
        sir = stack[stackstart % div];

        routsum -= sir[0];
        goutsum -= sir[1];
        boutsum -= sir[2];

        if (x == 0) {
          vmin[y] = Math.min(y + r1, hm) * w;
        }
        p = x + vmin[y];

        sir[0] = r[p];
        sir[1] = g[p];
        sir[2] = b[p];

        rinsum += sir[0];
        ginsum += sir[1];
        binsum += sir[2];

        rsum += rinsum;
        gsum += ginsum;
        bsum += binsum;

        stackpointer = (stackpointer + 1) % div;
        sir = stack[stackpointer];

        routsum += sir[0];
        goutsum += sir[1];
        boutsum += sir[2];

        rinsum -= sir[0];
        ginsum -= sir[1];
        binsum -= sir[2];

        yi += w;
      }
    }

    Log.e("pix", w + " " + h + " " + pix.length);
    bitmap.setPixels(pix, 0, w, 0, 0, w, h);
    return (bitmap);
  }
}