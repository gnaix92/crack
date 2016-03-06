package com.example.gnaix.crackme0201;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MainActivity extends AppCompatActivity {

    private EditText edit_userName;
    private EditText edit_sn;
    private Button btn_register;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setTitle(R.string.unregister); //未注册
        edit_userName = (EditText) findViewById(R.id.edit_username);
        edit_sn = (EditText) findViewById(R.id.edit_sn);
        btn_register = (Button) findViewById(R.id.button_register);

        btn_register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!checkSN(edit_userName.getText().toString().trim(),
                        edit_sn.getText().toString().trim())) {
                    //注册失败
                    Toast.makeText(MainActivity.this,
                            R.string.unsuccessed, Toast.LENGTH_SHORT).show();
                } else {
                    //注册成功
                    Toast.makeText(MainActivity.this,
                            R.string.successed, Toast.LENGTH_SHORT).show();
                    btn_register.setEnabled(false);
                    setTitle(R.string.registered);
                }
            }
        });

    }

    /**
     * 计算用户名和注册码是否匹配
     * @param userName
     * @param sn
     * @return
     */
    private boolean checkSN(String userName, String sn) {
        try {
            if (userName == null || userName.length() == 0) {
                return false;
            }

            if (sn == null || sn.length() == 0) {
                return false;
            }

            //MD5对用户名进行hash 最后计算出SN
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.reset();
            digest.update(userName.getBytes());
            byte[] bytes = digest.digest();
            String hexstr = toHexString(bytes, "");
            StringBuilder sb = new StringBuilder();
            for(int i=0; i<hexstr.length(); i+=2){
                sb.append(hexstr.charAt(i));
            }

            String userSN = sb.toString();
            if(!userSN.equalsIgnoreCase(sn)){ //比对SN
                return false;
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * hex 2 String
     * @param bytes
     * @param separator
     * @return
     */
    private static String toHexString(byte[] bytes, String separator){
        StringBuilder hexString = new StringBuilder();
        for(byte b : bytes){
            String hex = Integer.toHexString(0xFF & b);
            if(hex.length() == 1){
                hexString.append("0");
            }
            hexString.append(hex).append(separator);
        }
        return hexString.toString();
    }
}
