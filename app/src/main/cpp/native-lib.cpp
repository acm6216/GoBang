#include <jni.h>
#include <string>
#include <sys/socket.h>

#include <linux/in.h>
#include <linux/icmp.h>
#include <arpa/inet.h>
#include <android/log.h>
#include <cerrno>

static int borad[20*20];
extern "C" JNIEXPORT void JNICALL
Java_cen_xiaoyuan_gobang_GameHelper_pingByJNI(
        JNIEnv* env,jobject,
        jbyteArray chess_status,
        jint chess_board) {

    jbyte *carr = env->GetByteArrayElements(chess_status, NULL);
    for (int row=0;row<chess_board;row++){
        for(int col=0;col<chess_board;col++){
            int index = row*chess_board+col;
            borad[index]=carr[index];
        }
    }

    for (int row=0;row<chess_board;row++){
        char p[] = {"123456789123456"};
        for(int col=0;col<chess_board;col++){
            int index = row*chess_board+col;
            p[col]=borad[index]==0?'#':(borad[index]==1?'1':'0');
        }
        __android_log_print(ANDROID_LOG_DEBUG,"GoBang" ,"%s ",p);
    }
    __android_log_print(ANDROID_LOG_DEBUG,"GoBang" ,"\n ");

}