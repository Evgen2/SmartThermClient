package com.smartthermclient;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import com.google.android.material.button.MaterialButton;

public class ButtonCircle extends GradientDrawable {

    // Константы состояний для удобства чтения кода
    public static final int STATE_RED = 1;
    public static final int STATE_YELLOW = 2;
    public static final int STATE_GREEN = 3;
    private final MaterialButton targetButton;

    /**
     * Конструктор класса. Создает круг и сразу настраивает переданную кнопку (бывший setup_colorbutton).
     * @param button Кнопка, на которую нужно поместить светофор
     */
    public ButtonCircle(MaterialButton button)  {
        this.targetButton = button;
        // 1. Настраиваем форму круга и размеры
        setShape(GradientDrawable.OVAL);
        setSize(60, 60);
        setStroke(4, Color.GRAY); // Серый бортик

        // 2. Настраиваем радиальный градиент
        setGradientType(GradientDrawable.RADIAL_GRADIENT);
        setGradientRadius(30f);
        // 3. Привязка круга к кнопке и отключение системной маски
        targetButton.setIcon(this);
        targetButton.setIconTint(null);
    }

    /**
     * Меняет цвет градиента и текст на кнопке в зависимости от состояния
     * @param button Кнопка, к которой применяется состояние
     * @param state Номер состояния (1, 2 или 3)
     */
    public void change_colorbutton(int state) {
        int[] colors;

        switch (state) {
            case STATE_RED:
                colors = new int[]{ Color.parseColor("#FF6040"), Color.parseColor("#E01000") };
                //targetButton.setText("СТОП");
                break;

            case STATE_YELLOW:
                colors = new int[]{ Color.parseColor("#FFD880"), Color.parseColor("#F0A008") };
                //targetButton.setText("ЖДИТЕ");
                break;

            case STATE_GREEN:
                colors = new int[]{ Color.parseColor("#A9DFBF"), Color.parseColor("#1E8449") };
                //targetButton.setText("ИДИТЕ");
                break;

            default: // Серый цвет по умолчанию
                colors = new int[]{ Color.parseColor("#E5E7E9"), Color.parseColor("#7F8C8D") };
                //targetButton.setText("ВЫКЛ");
                break;
        }

        // Обновляем цвета самого круга
        setColors(colors);
    }
}
