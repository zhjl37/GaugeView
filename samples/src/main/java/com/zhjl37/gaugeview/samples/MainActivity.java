package com.zhjl37.gaugeview.samples;

import android.os.Bundle;

import com.zhjl37.gaugeview.GradeGaugeView.Adapter4Test;
import com.zhjl37.gaugeview.samples.databinding.MainActivityBinding;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private MainActivityBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = MainActivityBinding.inflate(getLayoutInflater());
        binding.gaugeView.setLabel("BMI");
        binding.gaugeView.setAdapter(new Adapter4Test());
        binding.gaugeView.setCurrent(20f);

        setContentView(binding.getRoot());
    }
}