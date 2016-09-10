
import java.util.ArrayList;
import java.util.List;

/**
 * Created by benjobs on 2016/9/10.
 */
public class SyncTest {


    public static void main(String[] args) throws InterruptedException {

        Thread jobThread = new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 4; i++) {
                    final int finalI = i;
                    Thread thread = new Thread(new Runnable() {
                        public void run() {
                                List<Integer> list = new ArrayList<Integer>();
                                list.add(1);
                                list.add(2);
                                list.add(2);
                                list.add(1);
                                list.add(2);
                                list.add(1);
                                list.add(2);

                                for (Integer vv : list) {
                                    try {
                                        runff(finalI, vv);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }

                                }
                        }
                    });
                    thread.start();
            }

            }
        });
        jobThread.start();

    }

    private static void runff(int index,Integer val) throws InterruptedException {

        synchronized (val) {
            System.out.println(index+"===>"+val);
            if (val==1) {
                Thread.sleep(2000);
            }
            /*if (val==2) {
                Thread.sleep(300);
            }*/
        }


    }

}
