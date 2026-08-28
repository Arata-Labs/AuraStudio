#include <jni.h>
#include <fcntl.h>
#include <sys/syscall.h>
#include <unistd.h>

/* The bootstrap zip is embedded in .rodata by termux-bootstrap-zip.S (.incbin). */
extern unsigned char blob[];
extern int blob_size;

#ifndef __NR_memfd_create
#if defined(__aarch64__)
#define __NR_memfd_create 279
#elif defined(__arm__)
#define __NR_memfd_create 385
#elif defined(__i386__)
#define __NR_memfd_create 354
#elif defined(__x86_64__)
#define __NR_memfd_create 319
#else
#error "memfd_create: unsupported architecture"
#endif
#endif

static int throw_runtime_exception(JNIEnv* env, char const* message)
{
    jclass exClass = (*env)->FindClass(env, "java/lang/RuntimeException");
    if (exClass != NULL) (*env)->ThrowNew(env, exClass, message);
    return -1;
}

/*
 * Copies the embedded bootstrap zip into an anonymous (memfd) file and returns
 * the file descriptor, seeked to the start.  Java then streams the zip straight
 * from the fd, avoiding a large in-memory byte array.
 * The fd is CLOEXEC so it is not leaked into shell sessions.
 */
JNIEXPORT jint JNICALL Java_com_termux_app_TermuxInstaller_getZipFd(JNIEnv *env, __attribute__((__unused__)) jobject This)
{
    int fd = (int) syscall(__NR_memfd_create, "aurastudio-bootstrap", 0);
    if (fd < 0) {
        return throw_runtime_exception(env, "memfd_create() failed");
    }

    size_t offset = 0;
    while (offset < (size_t) blob_size) {
        ssize_t n = write(fd, blob + offset, (size_t) blob_size - offset);
        if (n < 0) {
            close(fd);
            return throw_runtime_exception(env, "write() to memfd failed");
        }
        offset += (size_t) n;
    }

    if (lseek(fd, 0, SEEK_SET) < 0) {
        close(fd);
        return throw_runtime_exception(env, "lseek() on memfd failed");
    }
    fcntl(fd, F_SETFD, FD_CLOEXEC);
    return fd;
}