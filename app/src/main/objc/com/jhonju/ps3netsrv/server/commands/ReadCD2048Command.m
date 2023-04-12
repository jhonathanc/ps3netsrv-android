//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: ./app/src/main/java/com/jhonju/ps3netsrv/server/commands/ReadCD2048Command.java
//

#include "IOSPrimitiveArray.h"
#include "J2ObjC_source.h"
#include "com/jhonju/ps3netsrv/server/Context.h"
#include "com/jhonju/ps3netsrv/server/commands/AbstractCommand.h"
#include "com/jhonju/ps3netsrv/server/commands/ReadCD2048Command.h"
#include "com/jhonju/ps3netsrv/server/enums/CDSectorSize.h"
#include "java/io/ByteArrayOutputStream.h"
#include "java/io/File.h"
#include "java/io/RandomAccessFile.h"
#include "java/lang/IllegalArgumentException.h"
#include "java/lang/Throwable.h"

#if __has_feature(objc_arc)
#error "com/jhonju/ps3netsrv/server/commands/ReadCD2048Command must not be compiled with ARC (-fobjc-arc)"
#endif

@interface ComJhonjuPs3netsrvServerCommandsReadCD2048Command () {
 @public
  jint startSector_;
  jint sectorCount_;
}

- (IOSByteArray *)readSectorsWithJavaIoRandomAccessFile:(JavaIoRandomAccessFile *)file
                                               withLong:(jlong)offset
                                                withInt:(jint)count;

@end

inline jshort ComJhonjuPs3netsrvServerCommandsReadCD2048Command_get_MAX_RESULT_SIZE(void);
#define ComJhonjuPs3netsrvServerCommandsReadCD2048Command_MAX_RESULT_SIZE 2048
J2OBJC_STATIC_FIELD_CONSTANT(ComJhonjuPs3netsrvServerCommandsReadCD2048Command, MAX_RESULT_SIZE, jshort)

inline jint ComJhonjuPs3netsrvServerCommandsReadCD2048Command_get_MAX_SECTORS(void);
#define ComJhonjuPs3netsrvServerCommandsReadCD2048Command_MAX_SECTORS 2048
J2OBJC_STATIC_FIELD_CONSTANT(ComJhonjuPs3netsrvServerCommandsReadCD2048Command, MAX_SECTORS, jint)

__attribute__((unused)) static IOSByteArray *ComJhonjuPs3netsrvServerCommandsReadCD2048Command_readSectorsWithJavaIoRandomAccessFile_withLong_withInt_(ComJhonjuPs3netsrvServerCommandsReadCD2048Command *self, JavaIoRandomAccessFile *file, jlong offset, jint count);

@implementation ComJhonjuPs3netsrvServerCommandsReadCD2048Command

- (instancetype)initWithComJhonjuPs3netsrvServerContext:(ComJhonjuPs3netsrvServerContext *)ctx
                                                withInt:(jint)startSector
                                                withInt:(jint)sectorCount {
  ComJhonjuPs3netsrvServerCommandsReadCD2048Command_initWithComJhonjuPs3netsrvServerContext_withInt_withInt_(self, ctx, startSector, sectorCount);
  return self;
}

- (void)executeTask {
  if (sectorCount_ > ComJhonjuPs3netsrvServerCommandsReadCD2048Command_MAX_SECTORS) {
    @throw create_JavaLangIllegalArgumentException_initWithNSString_(@"Too many sectors read!");
  }
  if ([((ComJhonjuPs3netsrvServerContext *) nil_chk(ctx_)) getFile] == nil) {
    @throw create_JavaLangIllegalArgumentException_initWithNSString_(@"File shouldn't be null");
  }
  [self sendWithByteArray:ComJhonjuPs3netsrvServerCommandsReadCD2048Command_readSectorsWithJavaIoRandomAccessFile_withLong_withInt_(self, [((ComJhonjuPs3netsrvServerContext *) nil_chk(ctx_)) getReadOnlyFile], startSector_ * ((ComJhonjuPs3netsrvServerEnumsCDSectorSize *) nil_chk([((ComJhonjuPs3netsrvServerContext *) nil_chk(ctx_)) getCdSectorSize]))->cdSectorSize_, sectorCount_)];
}

- (IOSByteArray *)readSectorsWithJavaIoRandomAccessFile:(JavaIoRandomAccessFile *)file
                                               withLong:(jlong)offset
                                                withInt:(jint)count {
  return ComJhonjuPs3netsrvServerCommandsReadCD2048Command_readSectorsWithJavaIoRandomAccessFile_withLong_withInt_(self, file, offset, count);
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, NULL, 0x1, -1, 0, -1, -1, -1, -1 },
    { NULL, "V", 0x1, -1, -1, 1, -1, -1, -1 },
    { NULL, "[B", 0x2, 2, 3, 4, -1, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  #pragma clang diagnostic ignored "-Wundeclared-selector"
  methods[0].selector = @selector(initWithComJhonjuPs3netsrvServerContext:withInt:withInt:);
  methods[1].selector = @selector(executeTask);
  methods[2].selector = @selector(readSectorsWithJavaIoRandomAccessFile:withLong:withInt:);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "MAX_RESULT_SIZE", "S", .constantValue.asShort = ComJhonjuPs3netsrvServerCommandsReadCD2048Command_MAX_RESULT_SIZE, 0x1a, -1, -1, -1, -1 },
    { "MAX_SECTORS", "I", .constantValue.asInt = ComJhonjuPs3netsrvServerCommandsReadCD2048Command_MAX_SECTORS, 0x1a, -1, -1, -1, -1 },
    { "startSector_", "I", .constantValue.asLong = 0, 0x12, -1, -1, -1, -1 },
    { "sectorCount_", "I", .constantValue.asLong = 0, 0x12, -1, -1, -1, -1 },
  };
  static const void *ptrTable[] = { "LComJhonjuPs3netsrvServerContext;II", "LJavaIoIOException;LComJhonjuPs3netsrvServerExceptionsPS3NetSrvException;", "readSectors", "LJavaIoRandomAccessFile;JI", "LJavaIoIOException;" };
  static const J2ObjcClassInfo _ComJhonjuPs3netsrvServerCommandsReadCD2048Command = { "ReadCD2048Command", "com.jhonju.ps3netsrv.server.commands", ptrTable, methods, fields, 7, 0x1, 3, 4, -1, -1, -1, -1, -1 };
  return &_ComJhonjuPs3netsrvServerCommandsReadCD2048Command;
}

@end

void ComJhonjuPs3netsrvServerCommandsReadCD2048Command_initWithComJhonjuPs3netsrvServerContext_withInt_withInt_(ComJhonjuPs3netsrvServerCommandsReadCD2048Command *self, ComJhonjuPs3netsrvServerContext *ctx, jint startSector, jint sectorCount) {
  ComJhonjuPs3netsrvServerCommandsAbstractCommand_initWithComJhonjuPs3netsrvServerContext_(self, ctx);
  self->startSector_ = startSector;
  self->sectorCount_ = sectorCount;
}

ComJhonjuPs3netsrvServerCommandsReadCD2048Command *new_ComJhonjuPs3netsrvServerCommandsReadCD2048Command_initWithComJhonjuPs3netsrvServerContext_withInt_withInt_(ComJhonjuPs3netsrvServerContext *ctx, jint startSector, jint sectorCount) {
  J2OBJC_NEW_IMPL(ComJhonjuPs3netsrvServerCommandsReadCD2048Command, initWithComJhonjuPs3netsrvServerContext_withInt_withInt_, ctx, startSector, sectorCount)
}

ComJhonjuPs3netsrvServerCommandsReadCD2048Command *create_ComJhonjuPs3netsrvServerCommandsReadCD2048Command_initWithComJhonjuPs3netsrvServerContext_withInt_withInt_(ComJhonjuPs3netsrvServerContext *ctx, jint startSector, jint sectorCount) {
  J2OBJC_CREATE_IMPL(ComJhonjuPs3netsrvServerCommandsReadCD2048Command, initWithComJhonjuPs3netsrvServerContext_withInt_withInt_, ctx, startSector, sectorCount)
}

IOSByteArray *ComJhonjuPs3netsrvServerCommandsReadCD2048Command_readSectorsWithJavaIoRandomAccessFile_withLong_withInt_(ComJhonjuPs3netsrvServerCommandsReadCD2048Command *self, JavaIoRandomAccessFile *file, jlong offset, jint count) {
  jint SECTOR_SIZE = ((ComJhonjuPs3netsrvServerEnumsCDSectorSize *) nil_chk([((ComJhonjuPs3netsrvServerContext *) nil_chk(self->ctx_)) getCdSectorSize]))->cdSectorSize_;
  {
    JavaIoByteArrayOutputStream *out = create_JavaIoByteArrayOutputStream_initWithInt_(count * ComJhonjuPs3netsrvServerCommandsReadCD2048Command_MAX_RESULT_SIZE);
    JavaLangThrowable *__primaryException1 = nil;
    @try {
      for (jint i = 0; i < count; i++) {
        [((JavaIoRandomAccessFile *) nil_chk(file)) seekWithLong:offset + ComJhonjuPs3netsrvServerCommandsAbstractCommand_BYTES_TO_SKIP];
        IOSByteArray *sectorRead = [IOSByteArray arrayWithLength:ComJhonjuPs3netsrvServerCommandsReadCD2048Command_MAX_RESULT_SIZE];
        jint bytesLength = [file readWithByteArray:sectorRead];
        [out writeWithByteArray:sectorRead withInt:0 withInt:bytesLength];
        offset += SECTOR_SIZE;
      }
      return [out toByteArray];
    }
    @catch (JavaLangThrowable *e) {
      __primaryException1 = e;
      @throw e;
    }
    @finally {
      if (out != nil) {
        if (__primaryException1 != nil) {
          @try {
            [out close];
          }
          @catch (JavaLangThrowable *e) {
            [__primaryException1 addSuppressedWithJavaLangThrowable:e];
          }
        }
        else {
          [out close];
        }
      }
    }
  }
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(ComJhonjuPs3netsrvServerCommandsReadCD2048Command)
