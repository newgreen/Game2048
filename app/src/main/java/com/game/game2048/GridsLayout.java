package com.game.game2048;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

public class GridsLayout extends RelativeLayout {
    private int[] gridNumbers;
    private SingleGrid[] grids = null;
    private int layoutSize = 0;
    private int column = 4;

    private static final int FLING_MIN_DISTANCE = 50;
    private GestureDetector gestureDetector;

    public GridsLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        parseOnFlingMethod(context, attrs);

        gridNumbers = new int[column * column];
        for (int i = 1; i < gridNumbers.length; i++) {
            gridNumbers[i] = 1 << i;
        }

        gestureDetector = new GestureDetector(context , new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (isFling(e1, e2, velocityX, velocityY)) {
                    invokeOnFlingMethod(calculateFlingDirection(e1, e2, velocityX, velocityY));
                }
                return true;
            }
        });
    }

    public GridsLayout(Context context) {
        this(context, null, 0);
    }

    public GridsLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    // access the website for more information:
    // https://stackoverflow.com/questions/27462468/custom-view-overrides-ontouchevent-but-not-performclick
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        gestureDetector.onTouchEvent(event);
        return true;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        // square-shaped
        int length = Math.min(getMeasuredHeight(), getMeasuredWidth());
        if (needRefreshLayout(length)) {
            refreshLayout(length);
        }

        setMeasuredDimension(length, length);
    }

    void setGridNumbers(int[] gridNumbers) {
        int newColumn = (int)Math.sqrt(gridNumbers.length);
        if (gridNumbers.length != newColumn * newColumn) { // invalid gridNumbers, not square-shaped
            return;
        }

        this.gridNumbers = gridNumbers.clone();
        if (newColumn != column) { // column is changed and refresh layout
            refreshLayout(this.layoutSize);
            return;
        }

        if (grids != null) {
            updateGridNumbers();
        }
    }

    private void updateGridNumbers() {
        for (int i = 0; i < gridNumbers.length; i++) {
            grids[i].setNumber(gridNumbers[i]);
        }
    }

    private boolean needRefreshLayout(int layoutSize) {
        return grids == null || layoutSize != this.layoutSize || gridNumbers.length != column * column;
    }

    private void clearLayout() {
        if (grids != null) {
            for (SingleGrid grid:grids) {
                removeView(grid);
            }
            grids = null;
        }
    }

    private void refreshLayout(int layoutSize) {
        clearLayout();

        column = (int)Math.sqrt(gridNumbers.length);
        this.layoutSize = layoutSize;

        int margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                10, getResources().getDisplayMetrics());
        int padding = Arrays.stream(new int[]{
                getPaddingLeft(), getPaddingTop(), getPaddingRight(),getPaddingBottom()
        }).max().getAsInt();
        int gridSize = (layoutSize - (column - 1) * margin - 2 * padding) / column;

        grids = new SingleGrid[column * column];
        for (int i = 0; i < grids.length; i++) {
            grids[i] = new SingleGrid(getContext());

            RelativeLayout.LayoutParams params = new LayoutParams(gridSize, gridSize);
            if ((i + 1) % column != 0) { // not the rightmost grid
                params.rightMargin = margin;
            }
            if (i % column != 0) { // not the far left grid
                params.addRule(RelativeLayout.RIGHT_OF, grids[i - 1].getId());
            }
            if ((i + 1) > column) { // not the top row
                params.topMargin = margin;
                params.addRule(RelativeLayout.BELOW, grids[i - column].getId());
            }

            grids[i].setId(i + 1);
            addView(grids[i], params);
        }

        updateGridNumbers();
    }

    private Method onFlingMethod = null;
    private Object onFlingObj = null;

    private void parseOnFlingMethod(Context context, AttributeSet attrs) {
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.GridsLayout);
        String strMethodOnFling = array.getString(R.styleable.GridsLayout_onFling);
        array.recycle();

        Class<?> cls = context.getClass();
        try {
            if (strMethodOnFling != null) {
                onFlingMethod = cls.getMethod(strMethodOnFling, Direction.class);
                onFlingObj = context;
            }
        } catch (NoSuchMethodException e) {
            Log.e("onFling", "fail to get method onFling");
        }
    }

    private void invokeOnFlingMethod(Direction dir) {
        if (onFlingMethod != null && onFlingObj != null) {
            try {
                onFlingMethod.invoke(onFlingObj, dir);
            } catch (InvocationTargetException e) {
                Log.e("onFling", "invoking onFling invocation-target-ex: " + e.getMessage());
            } catch (IllegalAccessException e) {
                Log.e("onFling", "invoking onFling illegal-access-ex: " + e.getMessage());
            }
        }
    }

    private boolean isFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        return (Math.abs(e2.getX() - e1.getX()) > FLING_MIN_DISTANCE && Math.abs(velocityX) > Math.abs(velocityY)) ||
                (Math.abs(e2.getY() - e1.getY()) > FLING_MIN_DISTANCE && Math.abs(velocityX) < Math.abs(velocityY));
    }

    private Direction calculateFlingDirection(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        float x = e2.getX() - e1.getX();
        float y = e2.getY() - e1.getY();

        return (Math.abs(velocityX) > Math.abs(velocityY) && x < -FLING_MIN_DISTANCE) ? Direction.LEFT :
                (Math.abs(velocityX) > Math.abs(velocityY) && x > FLING_MIN_DISTANCE) ? Direction.RIGHT :
                        (Math.abs(velocityX) < Math.abs(velocityY) && y < -FLING_MIN_DISTANCE) ? Direction.UP :
                                (Math.abs(velocityX) < Math.abs(velocityY) && y > FLING_MIN_DISTANCE) ? Direction.DOWN : Direction.LEFT;
    }
}

class SingleGrid extends View {
    private int number = 0;
    private ColorScheme colorScheme;
    private Paint paint;
    private Rect bound;

    SingleGrid(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        // avoid allocations while drawing
        paint = new Paint();
        bound = new Rect();
        colorScheme = new ColorScheme();
    }

    SingleGrid(Context context) {
        this(context, null, 0);
    }

    void setNumber(int number) {
        if (number != this.number) {
            this.number = number;
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        paint.setColor(colorScheme.getColor(number).background());
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRect(0, 0, getWidth(), getHeight(), paint);

        if (number != 0) {
            String strNumber = String.valueOf(number);

            paint.setColor(colorScheme.getColor(number).foreground());
            paint.setTextSize(getTextSize()); // TODO: decrease the text size if the string is too long, or set it by the outer
            paint.getTextBounds(strNumber, 0, strNumber.length(), bound);

            float x = (getWidth() - bound.width()) / 2.0f;
            float y = getHeight() / 2.0f + bound.height() / 2.0f;
            canvas.drawText(strNumber, x, y, paint);
        }
    }

    private float getTextSize() {
        return (String.valueOf(number).length() > 4) ? 65 : 80;
    }
}

class ColorScheme {
    int foreground() {
        return Color.parseColor(strForeground);
    }

    int background() {
        return Color.parseColor(strBackground);
    }

    ColorScheme getColor(int number) {
        switch (number) {
            case 0:
                setColor("#FFFFFFFF", "#FFCCC0B3");
                break;
            case 2:
                setColor("#FF000000", "#FFEEE4DA");
                break;
            case 4:
                setColor("#FF000000", "#FFEDE0C8");
                break;
            case 8:
                setColor("#FFFFFFFF", "#FFF2B179");
                break;
            case 16:
                setColor("#FFFFFFFF", "#FFF49563");
                break;
            case 32:
                setColor("#FFFFFFFF", "#FFF5794D");
                break;
            case 64:
                setColor("#FFFFFFFF", "#FFF55D37");
                break;
            case 128:
                setColor("#FF000000", "#FFEEE863");
                break;
            case 256:
                setColor("#FFFFFFFF", "#FFEDB04D");
                break;
            case 512:
                setColor("#FFFFFFFF", "#FFECB04D");
                break;
            case 1024:
                setColor("#FFFFFFFF", "#FFEB9437");
                break;
            case 2048:
                setColor("#FF000000", "#FF80FF00");
                break;
            case 4096:
                setColor("#FF000000", "#FF00E83A");
                break;
            default:
                setColor("#FFFFFFFF", "#FF00771E");
                break;
        }
        return this;
    }

    private void setColor(String foreground, String background) {
        strForeground = foreground;
        strBackground = background;
    }

    private String strForeground = "#FF000000";
    private String strBackground = "#FF000000";
}
