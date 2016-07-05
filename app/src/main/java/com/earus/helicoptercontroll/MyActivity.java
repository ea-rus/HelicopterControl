package com.earus.helicoptercontroll;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.SeekBar;
import android.widget.TextView;

import com.zerokol.views.JoystickView;

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

    private int calibration;
    private VerticalSeekBar throttle;
    private VerticalSeekBar direct_forward;
    private SeekBar direct_side;
    private EditText textlog;


    private Object irService;
    private Class irClass;
    private Method sendIR;
    private Method readIR;
    private JoystickView joystick;
    private JoystickView joystick_throttle;

    private void Alert(Exception e){
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        String exceptionAsString = sw.toString();

        new AlertDialog.Builder(this)
                .setMessage(exceptionAsString)
                .show();
    }


    Timer irTimer;



    public static final String PREFS_NAME = "HelicopterControll";


    float c_tmin,c_tmax, c_left, c_right, c_fwd, c_back;
    Integer c_calib;
    long joystickinterval;




    public void Save( View view){
        String config=configedit.getText().toString();

        String [] ar = config.split("\n");

        String [] ar2;

        ar2=ar[0].split(" ");
        c_tmin= Integer.parseInt(ar2[0]);
        c_tmax= Integer.parseInt(ar2[1]);

        ar2=ar[1].split(" ");
        c_left= Integer.parseInt(ar2[0]);
        c_right= Integer.parseInt(ar2[1]);

        ar2=ar[2].split(" ");
        c_back= Integer.parseInt(ar2[0]);
        c_fwd= Integer.parseInt(ar2[1]);

        c_calib= Integer.parseInt(ar[3]);

         joystickinterval =  Integer.parseInt(ar[4]);

        //return joystickinterval;
    }

    EditText configedit ;

//    @Override
//    public boolean onKeyDown(int keyCode, KeyEvent event) {
//        boolean handled = false;
//
//        textlog.setText(Integer.toString(keyCode)+" : "+event.getSource()+" : "+InputDevice.SOURCE_GAMEPAD +" : "+ event.getRepeatCount())  ;
//
//
//        //return super.onKeyDown(keyCode, event);
//        return true;
//    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        String config = settings.getString("config", null);



        if (config ==null){
             config="0 255\n" + //Throttle
                    "-64 63\n" +//LeftRight
                    "-128 127\n" +//FwdBack
                    "52\n" + //calibration
                    "200"; //joystick interval

        }

        configedit = (EditText) findViewById(R.id.config);

        configedit.setText(config);

        Save(joystick);



        textlog = (EditText) findViewById(R.id.textlog);

//
//        Button button_joystick = (Button) findViewById(R.id.button_joystick);
//        button_joystick.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//
//            }
//        });




        joystick = (JoystickView) findViewById(R.id.joystickView);
        joystick_throttle = (JoystickView) findViewById(R.id.joystickView_throttle);



        joystick_throttle.setOnJoystickMoveListener(new JoystickView.OnJoystickMoveListener() {

            @Override
            public void onValueChanged(int angle, int pwr, int direction) {

               // if (power>3 && Math.abs(angle)<180) {

                float p = -joystick_throttle.getYpos();
                int power = (int) (p* (c_tmax-c_tmin)/200 + ((c_tmax+c_tmin)/2));// 0..100 to c*

                power=Math.abs(power);

                p = joystick.getXpos();
                int leftRight = (int) ( p* (c_right-c_left)/200 + (c_right+c_left)/2);

                p =-joystick.getYpos();
                int forwardBackward = (int) (p* (c_fwd-c_back)/200 + (c_fwd+c_back)/2);


                    String irCode = mkCommand(power, leftRight, forwardBackward, c_calib);

                    try {
                        sendIR.invoke(irService, irCode);
                    } catch (Exception e) {
                        Alert(e);
                        e.printStackTrace();
                    }
                    //Log.d("ir", irCode);
                    textlog.setText(Integer.toString(power)+" : "+Integer.toString(leftRight)+" : "+Integer.toString(forwardBackward));

             //   }

                //textlog.append(Integer.toString(power)+"-"+Integer.toString(joystick.getXpos())+"-"+Integer.toString(joystick.getYpos())+"\n");


            }
        }, joystickinterval);

        //this.clear(null);
//        text_code = //(TextView) findViewById(R.id.text_code);

        try{

            Context context=this.getApplicationContext();
            Class contClass=context.getClass();

            String ir_service_name=(String) contClass.getField("CONSUMER_IR_SERVICE").get(context);
            irService =  this.getSystemService(ir_service_name);
            irClass = irService.getClass();



            sendIR = irClass.getMethod("write_irsend", new Class[]{String.class});
            readIR = irClass.getMethod("read_irsend");

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


//        irTimer.schedule(new TimerTask() { // Определяем задачу
//            @Override
//            public void run() {
//                uiHandler.post(new Runnable() {
//                    @Override
//                    public void run() {
//                        int t = throttle.getProgress();
//                        if (t>3) {
//                            String irCode = mkCommand(t, direct_side.getProgress(), direct_forward.getProgress(), calibration.getValue());
//                            textlog.append(Integer.toString(t)+"\n");
//                            try {
//                                sendIR.invoke(irService, irCode);
//                            } catch (Exception e) {
//                                Alert(e);
//                                e.printStackTrace();
//                            }
//
//                        }
//                    }
//                });
//            }
//
//            ;
//        }, 500, 180); // интервал - 60000 миллисекунд, 0 миллисекунд до первого запуска.
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
        textlog.setText("");
//        //throttle.setProgress(0);
//        direct_forward.setProgress(50);
//        direct_side.setProgress(50);
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

    protected void onStop() {
        super.onStop();

        EditText config = (EditText) findViewById(R.id.config);
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        String configstr = config.getText().toString();
        editor.putString("config", configstr);
        editor.commit();

    }
}

