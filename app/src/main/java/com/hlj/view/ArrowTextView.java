package com.hlj.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.TextView;

public class ArrowTextView extends TextView {

    private Paint paint,line;
    private int bgColor;

    public void setBgColor(int bgColor) {
        this.bgColor = bgColor;
        if (paint != null) {
            paint.setColor(bgColor);
        }
        postInvalidate();
    }

    public ArrowTextView(Context context) {
        super(context);
        init();
    }

    public ArrowTextView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ArrowTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStrokeWidth(2);
        paint.setColor(bgColor);

        line = new Paint();
        line.setAntiAlias(true);
        line.setStrokeWidth(2);
        line.setColor(Color.WHITE);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int height = getHeight();   //获取View的高度
        int width = getWidth();     //获取View的宽度

        //框定文本显示的区域
        canvas.drawRoundRect(new RectF(getPaddingLeft()-7,getPaddingTop()-2,width-getPaddingRight()+7,height-getPaddingBottom()+2),10,10,line);
        canvas.drawRoundRect(new RectF(getPaddingLeft()-5,getPaddingTop(),width-getPaddingRight()+5,height-getPaddingBottom()),10,10,paint);

        Path linePath = new Path();
        //以下是绘制文本的那个箭头
        linePath.moveTo(width / 2, height+2);// 三角形顶点
        linePath.lineTo(width / 2 - 17, height - getPaddingBottom());   //三角形左边的点
        linePath.lineTo(width / 2 + 17, height - getPaddingBottom());   //三角形右边的点
        linePath.close();
        canvas.drawPath(linePath, line);

        Path path = new Path();
        //以下是绘制文本的那个箭头
        path.moveTo(width / 2, height);// 三角形顶点
        path.lineTo(width / 2 - 15, height - getPaddingBottom());   //三角形左边的点
        path.lineTo(width / 2 + 15, height - getPaddingBottom());   //三角形右边的点
        path.close();
        canvas.drawPath(path, paint);

        super.onDraw(canvas);
    }

}
