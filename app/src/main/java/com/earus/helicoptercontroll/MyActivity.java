package com.earus.helicoptercontroll;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Canvas;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.Timer;
import java.util.TimerTask;


/*
No. Throttle  Direction                  Binary                              Dec(Hex)
00    ~0%      Middle       00111100 00111111 10001011 00110100    60(3C)  63(3F)  139(8B) 52(34)
01   ~25%      Middle       00111100 00111111 10100111 00110100    60(3C)  63(3F)  167(A7) 52(34)
02   ~50%      Middle       00111100 00111111 11001000 00110100    60(3C)  63(3F)  200(C8) 52(34)
03   ~75%      Middle       00111100 00111111 11100110 00110100    60(3C)  63(3F)  230(E6) 52(34)
04   100%      Middle       00111100 00111111 11111101 00110100    60(3C)  63(3F)  253(FD) 52(34)
05   100%      Left         01101010 00111111 11111101 00110100    106(6A) 63(3F)  253(FD) 52(34)
06   100%      Right        00001000 00111111 11111101 00110100    8(8)    63(3F)  253(FD) 52(34)
07   100%      Forward      01000000 00000001 11111101 00110100    64(40)  1(1)    253(FD) 52(34)
08   100%      Backward     00111100 01111110 11111111 00110100    60(3C)  126(7E) 255(FF) 52(34)
09    ~0%      Left         01101010 00111111 10001101 00110100    106(6A) 63(3F)  141(8D) 52(34)
10    ~0%      Right        00001000 00111111 10001101 00110100    8(8)    63(3F)  141(8D) 52(34)
11    ~0%      Forward      01000000 00000001 10001101 00110100    64(40)  1(1)    141(8D) 52(34)
12    ~0%      Backward     00111100 01111101 10010001 00110100    60(3C)  125(7D) 145(91) 52(34)
* */

public class MyActivity extends Activity {

    private NumberPicker calibration;
    private VerticalSeekBar throttle;
    private VerticalSeekBar direct_forward;
    private SeekBar direct_side;
    private EditText textlog;


    private Object irService;
    private Class irClass;
    private Method sendIR;
    private Method readIR;


    private void Alert(Exception e){
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        String exceptionAsString = sw.toString();

        new AlertDialog.Builder(this)
                .setMessage(exceptionAsString)
                .show();
    }


    Timer irTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);

        calibration = (NumberPicker) findViewById(R.id.calibration);
        throttle = (VerticalSeekBar) findViewById(R.id.throttle);
        direct_forward = (VerticalSeekBar) findViewById(R.id.direct_forward);
        direct_side = (SeekBar) findViewById(R.id.direct_side);
        textlog = (EditText) findViewById(R.id.textlog);



        calibration.setMaxValue(100);
        calibration.setMinValue(0);
        calibration.setMinValue(52);

        calibration.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        this.clear(null);
//        text_code = //(TextView) findViewById(R.id.text_code);

        try{

            irService =  this.getSystemService("irda");
            irClass = irService.getClass();
    
    
            readIR = irClass.getMethod("read_irsend");
            sendIR = irClass.getMethod("write_irsend", new Class[]{String.class});

        } catch (Exception e) {
            Alert( e );
            e.printStackTrace();
        }




        
        

//                if(irCode.startsWith("0000 "))
//                    irCode = convertProntoHexStringToIntString(irCode);
//                try {
//                    sendIR.invoke(irService, irCode);
//                } catch (Exception e) {
//                    Alert( e );
//                    e.printStackTrace();
//                }

    }
    @Override
    protected void onPause () {
        super.onPause();
        Log.i("evnt","onPause");
        irTimer.cancel();
    }
    @Override
    protected void onResume () {
        super.onResume();
        Log.i("evnt","onResume");
        irTimer = new Timer(); // Создаем таймер
        final Handler uiHandler = new Handler();


        irTimer.schedule(new TimerTask() { // Определяем задачу
            @Override
            public void run() {
                uiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        int t = throttle.getProgress();
                        if (t>3) {
                            String irCode = mkCommand(t, direct_side.getProgress(), direct_forward.getProgress(), calibration.getValue());
                            textlog.append(Integer.toString(t)+"\n");
                            try {
                                sendIR.invoke(irService, irCode);
                            } catch (Exception e) {
                                Alert(e);
                                e.printStackTrace();
                            }

                        }
                    }
                });
            }

            ;
        }, 500, 180); // интервал - 60000 миллисекунд, 0 миллисекунд до первого запуска.
    }


    private int ROTATION_STATIONARY=60;


    private String mkCommand(int throttle, int leftRight, int forwardBackward, int calibr)
    {
        leftRight=(int)( (double)leftRight*1.27 - 64 + ROTATION_STATIONARY);

        throttle=(int)( (double)throttle*2.55);

        forwardBackward=(int)( (double)forwardBackward*2.55 - 128);

        String out="38000,76,76,14";

        int b;

        for (int i = 7; i >=0; i--)
        {
            b = ((leftRight & (1 << i)) >> i );
            if (b > 0) out+=",23,14"; else  out+=",8,14";
        }

        for (int i = 7; i >=0; i--)
        {
            b = ((63 + forwardBackward) & (1 << i)) >> i;
            if (b > 0) out+=",23,14"; else  out+=",8,14";
        }

        for (int i = 7; i >=0; i--)
        {
            b = (throttle & (1 << i)) >> i;
            if (b > 0) out+=",23,14"; else  out+=",8,14";
        }

        for (int i = 7; i >=0; i--)
        {
            b = (calibr & (1 << i)) >> i;
            if (b > 0) out+=",23,14"; else  out+=",8,14";
        }
        return out;
    }

    public void clear (View view){
        throttle.setProgress(0);
        direct_forward.setProgress(50);
        direct_side.setProgress(50);
    }


    public static String convertProntoHexStringToIntString(String s) {
        String[] codes = s.split(" ");
        StringBuilder sb = new StringBuilder();
        sb.append(getFrequency(codes[1]) + ",");
        for (int i = 4; i < codes.length; i++) {
            sb.append(Integer.parseInt(codes[i], 16) + ",");
        }
        return sb.toString();
    }

    public static String getFrequency(String s) {
        int val = Integer.parseInt(s, 16);
        Integer i = (int) (1000000 / (val * .241246));
        return i.toString();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.my, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

