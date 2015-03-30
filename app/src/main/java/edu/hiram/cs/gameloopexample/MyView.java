package edu.hiram.cs.gameloopexample;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.media.MediaPlayer;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.Random;

public class MyView extends SurfaceView implements SurfaceHolder.Callback {
    private static final int NUM_ROWS = 3;
    private static final int NUM_COLS = 10;
    private static final int TOP_MARGIN = 50;
    private static final int PADDLE_THICKNESS=20;
    private static final int PADDLE_LENGTH=150;
    private static final int BALL_SIZE=25;
    private Rect rect;

    //state variables
    private int xCenter,yCenter;
    private int xPaddle,yPaddle;
    private double xVel,yVel;
    private Paint bgPaint,ballPaint;
    private boolean bounce;
    private MyThread thread;
    private ArrayList<Point> blocks; //upper left hand corner of the blocks
    private int blockWidth;
    private int blockHeight;
    MediaPlayer player;
    MediaPlayer player2;



    public MyView(Context context) {
        super(context);
        init(null, 0);
    }

    public MyView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public MyView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        player=MediaPlayer.create(getContext(), R.raw.bounce);
        player2=MediaPlayer.create(getContext(), R.raw.paddlebounce);
        blocks=new ArrayList<Point>();
        xCenter=50;
        yCenter=50;
        xPaddle=50;
        yPaddle=1000;
        xVel=10.0;
        yVel=25.0;
        bgPaint=new Paint();
        bgPaint.setColor(Color.BLACK);
        ballPaint=new Paint();
        ballPaint.setColor(Color.RED);
        thread=new MyThread();
        getHolder().addCallback(this);
        setFocusable(true);
        bounce=false;
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawRect(0,0,getWidth(),getHeight(),bgPaint);
        canvas.drawCircle(xCenter,yCenter,BALL_SIZE,ballPaint);
        canvas.drawRect(xPaddle,yPaddle,xPaddle+PADDLE_LENGTH,yPaddle+PADDLE_THICKNESS,ballPaint);
        for(Point p:blocks) {
            canvas.drawRect(p.x,p.y,p.x+blockWidth-2,p.y+blockHeight-2,ballPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        rect.left=xPaddle;
        rect.right=xPaddle+PADDLE_LENGTH;
        xPaddle=(int)event.getX()-PADDLE_LENGTH/2;
        return true;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        blockWidth=getWidth()/NUM_COLS;
        blockHeight=getHeight()/50;
        for(int i=0;i<NUM_ROWS;i++) {
            for(int j=0;j<NUM_COLS;j++) {
                blocks.add(new Point(j*blockWidth,i*blockHeight+TOP_MARGIN));
            }
        }
        yPaddle=getHeight()-PADDLE_THICKNESS*6;
        xPaddle=getWidth()/2-PADDLE_LENGTH/2;
        rect = new Rect(xPaddle,yPaddle,xPaddle+PADDLE_LENGTH,yPaddle+PADDLE_THICKNESS);
        thread.setRunning(true);
        thread.start();

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        boolean retry=true;
        while(retry) {
            try {
                thread.join();
                retry=false;
            } catch (InterruptedException e) {
                //just do nothing so go to the top of while loop
            }
        }

    }

    class MyThread extends Thread {

        private boolean running;
        private Random rand;

        public MyThread() {
            rand=new Random();
        }

        public void setRunning(boolean b) {
            running=b;
        }


        
        private int hit(Point p, int x, int y) {
            //P top left corner of block
            //x and y are the ball coordinates
            if (x>p.x && x<p.x+blockWidth &&
                    y>p.y && y<p.y+blockHeight){
                return 2;
            }


            //0 = bounce x (hits sides)
            //1 = bounce y (hits bottom)
            //2 = bounce both (hits corner)

            return -1;
        }

        @Override
        public void run() {
            while(running) {
                //update state
                xCenter+=xVel;
                yCenter+=yVel;
                if (xCenter<0 || xCenter>getWidth()) {
                    xVel=-xVel;
                }
                if (yCenter<0 || yCenter>getHeight()) {
                    yVel=-yVel;
                }
                //see if ball has hit paddle (or gone below it)
                if (!bounce &&
                        rect.contains(xCenter,yCenter+BALL_SIZE)||
                        rect.contains(xCenter-BALL_SIZE,yCenter)||
                        rect.contains(xCenter+BALL_SIZE,yCenter)||
                        rect.contains(xCenter,yCenter-BALL_SIZE)){
                    if (player2.isPlaying()){
                        player2.seekTo(0);
                    } else {
                        player2.start();
                    }
                    yVel=-yVel;
                    yVel+=(rand.nextInt(10)-5);
                    xVel+=(rand.nextInt(10)-5);
                    bounce=true;
                }else {
                    bounce=false;
                }
                //see if ball has hit a block
                for(int i=0;i<blocks.size();i++) {
                    Point p=blocks.get(i);
                    int edge=hit(p,xCenter,yCenter);
                    if (edge>=0) {
                        if (player.isPlaying()){
                            player.seekTo(0);
                        } else {
                            player.start();
                        }
                        blocks.remove(i);
                        switch (edge) {
                            case 0: //bounce x
                                xVel=-xVel;
                                break;
                            case 1: //bounce y
                                yVel=-yVel;
                                break;
                            case 2: //bounce both
                                xVel=-xVel;
                                yVel=-yVel;
                                break;
                        }
                        break;
                    }
                }



                //redraw the screen
                Canvas canvas=getHolder().lockCanvas();
                if (canvas!=null) {
                    synchronized (canvas) {
                        onDraw(canvas);
                    }
                    getHolder().unlockCanvasAndPost(canvas);
                }

                //wait for a short amount of time
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                }
            }
        }


    }
}












