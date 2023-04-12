//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: ./app/src/main/java/com/jhonju/ps3netsrv/server/commands/ReadDirEntryCommand.java
//

#include "IOSObjectArray.h"
#include "IOSPrimitiveArray.h"
#include "J2ObjC_source.h"
#include "com/jhonju/ps3netsrv/server/Context.h"
#include "com/jhonju/ps3netsrv/server/commands/AbstractCommand.h"
#include "com/jhonju/ps3netsrv/server/commands/IResult.h"
#include "com/jhonju/ps3netsrv/server/commands/ReadDirEntryCommand.h"
#include "com/jhonju/ps3netsrv/server/utils/Utils.h"
#include "java/io/ByteArrayOutputStream.h"
#include "java/io/File.h"
#include "java/lang/Throwable.h"
#include "java/nio/charset/Charset.h"
#include "java/nio/charset/StandardCharsets.h"

#if __has_feature(objc_arc)
#error "com/jhonju/ps3netsrv/server/commands/ReadDirEntryCommand must not be compiled with ARC (-fobjc-arc)"
#endif

inline jint ComJhonjuPs3netsrvServerCommandsReadDirEntryCommand_get_RESULT_LENGTH(void);
#define ComJhonjuPs3netsrvServerCommandsReadDirEntryCommand_RESULT_LENGTH 266
J2OBJC_STATIC_FIELD_CONSTANT(ComJhonjuPs3netsrvServerCommandsReadDirEntryCommand, RESULT_LENGTH, jint)

inline jshort ComJhonjuPs3netsrvServerCommandsReadDirEntryCommand_get_MAX_FILE_NAME_LENGTH(void);
#define ComJhonjuPs3netsrvServerCommandsReadDirEntryCommand_MAX_FILE_NAME_LENGTH 255
J2OBJC_STATIC_FIELD_CONSTANT(ComJhonjuPs3netsrvServerCommandsReadDirEntryCommand, MAX_FILE_NAME_LENGTH, jshort)

inline jshort ComJhonjuPs3netsrvServerCommandsReadDirEntryCommand_get_EMPTY_FILE_NAME_LENGTH(void);
#define ComJhonjuPs3netsrvServerCommandsReadDirEntryCommand_EMPTY_FILE_NAME_LENGTH 0
J2OBJC_STATIC_FIELD_CONSTANT(ComJhonjuPs3netsrvServerCommandsReadDirEntryCommand, EMPTY_FILE_NAME_LENGTH, jshort)

@interface ComJhonjuPs3netsrvServerCommandsReadDirEntryCommand_ReadDirEntryResult : NSObject < ComJhonjuPs3netsrvServerCommandsIResult > {
 @public
  jlong aFileSize_;
  jshort bFileNameLength_;
  jboolean cIsDirectory_;
  NSString *dFileName_;
}

- (instancetype)init;

- (instancetype)initWithLong:(jlong)aFileSize
                   withShort:(jshort)bFileNameLength
                 withBoolean:(jboolean)cIsDirectory
                withNSString:(NSString *)dFileName;

- (IOSByteArray *)toByteArray;

@end

J2OBJC_EMPTY_STATIC_INIT(ComJhonjuPs3netsrvServerCommandsReadDirEntryCommand_ReadDirEntryResult)

J2OBJC_FIELD_SETTER(ComJhonjuPs3netsrvServerCommandsReadDirEntryCommand_ReadDirEntryResult, dFileName_, NSString *)

__attribute__((unused)) static void ComJhonjuPs3netsrvServerCommandsReadDirEntryCommand_ReadDirEntryResult_init(ComJhonjuPs3netsrvServerCommandsReadDirEntryCommand_ReadDirEntryResult *self);

__attribute__((unused)) static ComJhonjuPs3netsrvServerCommandsReadDirEntryCommand_ReadDirEntryResult *new_ComJhonjuPs3netsrvServerCommandsReadDirEntryCommand_ReadDirEntryResult_init(void) NS_RETURNS_RETAINED;

__attribute__((unused)) static ComJhonjuPs3netsrvServerCommandsReadDirEntryCommand_ReadDirEntryResult *create_ComJhonjuPs3netsrvServerCommandsReadDirEntryCommand_ReadDirEntryResult_init(void);

__attribute__((unused)) static void ComJhonjuPs3netsrvServerCommandsReadDirEntryCommand_ReadDirEntryResult_initWithLong_withShort_withBoolean_withNSString_(ComJhonjuPs3netsrvServerCommandsReadDirEntryCommand_ReadDirEntryResult *self, jlong aFileSize, jshort bFileNameLength, jboolean cIsDirectory, NSString *dFileName);

__attribute__((unused)) static ComJhonjuPs3netsrvServerCommandsReadDirEntryCommand_ReadDirEntryResult *new_ComJhonjuPs3netsrvServerCommandsReadDirEntryCommand_ReadDirEntryResult_initWithLong_withShort_withBoolean_withNSString_(jlong aFileSize, jshort bFileNameLength, jboolean cIsDirectory, NSString *dFileName) NS_RETURNS_RETAINED;

__attribute__((unused)) static ComJhonjuPs3netsrvServerCommandsReadDirEntryCommand_ReadDirEntryResult *create_ComJhonjuPs3netsrvServerCommandsReadDirEntryCommand_ReadDirEntryResult_initWithLong_withShort_withBoolean_withNSString_(jlong aFileSize, jshort bFileNameLength, jboolean cIsDirectory, NSString *dFileName);

J2OBJC_TYPE_LITERAL_HEADER(ComJhonjuPs3netsrvServerCommandsReadDirEntryCommand_ReadDirEntryResult)

@implementation ComJhonjuPs3netsrvServerCommandsReadDirEntryCommand

- (instancetype)initWithComJhonjuPs3netsrvServerContext:(ComJhonjuPs3netsrvServerContext *)ctx {
  ComJhonjuPs3netsrvServerCommandsReadDirEntryCommand_initWithComJhonjuPs3netsrvServerContext_(self, ctx);
  return self;
}

- (void)executeTask {
  JavaIoFile *file = JreRetainedLocalValue([((ComJhonjuPs3netsrvServerContext *) nil_chk(ctx_)) getFile]);
  if (file == nil || ![file isDirectory]) {
    [self sendWithComJhonjuPs3netsrvServerCommandsIResult:create_ComJhonjuPs3netsrvServerCommandsReadDirEntryCommand_ReadDirEntryResult_init()];
    return;
  }
  JavaIoFile *fileAux = nil;
  IOSObjectArray *fileList = [file list];
  if (fileList != nil) {
    {
      IOSObjectArray *a__ = fileList;
      NSString * const *b__ = a__->buffer_;
      NSString * const *e__ = b__ + a__->size_;
      while (b__ < e__) {
        NSString *fileName = *b__++;
        fileAux = create_JavaIoFile_initWithNSString_(JreStrcat("$C$", [file getCanonicalPath], '/', fileName));
        if ([((NSString *) nil_chk([fileAux getName])) java_length] <= ComJhonjuPs3netsrvServerCommandsReadDirEntryCommand_MAX_FILE_NAME_LENGTH) {
          break;
        }
      }
    }
  }
  if (fileAux == nil) {
    [((ComJhonjuPs3netsrvServerContext *) nil_chk(ctx_)) setFileWithJavaIoFile:nil];
    [self sendWithComJhonjuPs3netsrvServerCommandsIResult:create_ComJhonjuPs3netsrvServerCommandsReadDirEntryCommand_ReadDirEntryResult_init()];
    return;
  }
  [self sendWithComJhonjuPs3netsrvServerCommandsIResult:create_ComJhonjuPs3netsrvServerCommandsReadDirEntryCommand_ReadDirEntryResult_initWithLong_withShort_withBoolean_withNSString_([fileAux isDirectory] ? ComJhonjuPs3netsrvServerCommandsAbstractCommand_EMPTY_SIZE : [file length], (jshort) [((NSString *) nil_chk([fileAux getName])) java_length], [fileAux isDirectory], [fileAux getName])];
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, NULL, 0x1, -1, 0, -1, -1, -1, -1 },
    { NULL, "V", 0x1, -1, -1, 1, -1, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  #pragma clang diagnostic ignored "-Wundeclared-selector"
  methods[0].selector = @selector(initWithComJhonjuPs3netsrvServerContext:);
  methods[1].selector = @selector(executeTask);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "RESULT_LENGTH", "I", .constantValue.asInt = ComJhonjuPs3netsrvServerCommandsReadDirEntryCommand_RESULT_LENGTH, 0x1a, -1, -1, -1, -1 },
    { "MAX_FILE_NAME_LENGTH", "S", .constantValue.asShort = ComJhonjuPs3netsrvServerCommandsReadDirEntryCommand_MAX_FILE_NAME_LENGTH, 0x1a, -1, -1, -1, -1 },
    { "EMPTY_FILE_NAME_LENGTH", "S", .constantValue.asShort = ComJhonjuPs3netsrvServerCommandsReadDirEntryCommand_EMPTY_FILE_NAME_LENGTH, 0x1a, -1, -1, -1, -1 },
  };
  static const void *ptrTable[] = { "LComJhonjuPs3netsrvServerContext;", "LJavaIoIOException;LComJhonjuPs3netsrvServerExceptionsPS3NetSrvException;", "LComJhonjuPs3netsrvServerCommandsReadDirEntryCommand_ReadDirEntryResult;" };
  static const J2ObjcClassInfo _ComJhonjuPs3netsrvServerCommandsReadDirEntryCommand = { "ReadDirEntryCommand", "com.jhonju.ps3netsrv.server.commands", ptrTable, methods, fields, 7, 0x1, 2, 3, -1, 2, -1, -1, -1 };
  return &_ComJhonjuPs3netsrvServerCommandsReadDirEntryCommand;
}

@end

void ComJhonjuPs3netsrvServerCommandsReadDirEntryCommand_initWithComJhonjuPs3netsrvServerContext_(ComJhonjuPs3netsrvServerCommandsReadDirEntryCommand *self, ComJhonjuPs3netsrvServerContext *ctx) {
  ComJhonjuPs3netsrvServerCommandsAbstractCommand_initWithComJhonjuPs3netsrvServerContext_(self, ctx);
}

ComJhonjuPs3netsrvServerCommandsReadDirEntryCommand *new_ComJhonjuPs3netsrvServerCommandsReadDirEntryCommand_initWithComJhonjuPs3netsrvServerContext_(ComJhonjuPs3netsrvServerContext *ctx) {
  J2OBJC_NEW_IMPL(ComJhonjuPs3netsrvServerCommandsReadDirEntryCommand, initWithComJhonjuPs3netsrvServerContext_, ctx)
}

ComJhonjuPs3netsrvServerCommandsReadDirEntryCommand *create_ComJhonjuPs3netsrvServerCommandsReadDirEntryCommand_initWithComJhonjuPs3netsrvServerContext_(ComJhonjuPs3netsrvServerContext *ctx) {
  J2OBJC_CREATE_IMPL(ComJhonjuPs3netsrvServerCommandsReadDirEntryCommand, initWithComJhonjuPs3netsrvServerContext_, ctx)
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(ComJhonjuPs3netsrvServerCommandsReadDirEntryCommand)

@implementation ComJhonjuPs3netsrvServerCommandsReadDirEntryCommand_ReadDirEntryResult

J2OBJC_IGNORE_DESIGNATED_BEGIN
- (instancetype)init {
  ComJhonjuPs3netsrvServerCommandsReadDirEntryCommand_ReadDirEntryResult_init(self);
  return self;
}
J2OBJC_IGNORE_DESIGNATED_END

- (instancetype)initWithLong:(jlong)aFileSize
                   withShort:(jshort)bFileNameLength
                 withBoolean:(jboolean)cIsDirectory
                withNSString:(NSString *)dFileName {
  ComJhonjuPs3netsrvServerCommandsReadDirEntryCommand_ReadDirEntryResult_initWithLong_withShort_withBoolean_withNSString_(self, aFileSize, bFileNameLength, cIsDirectory, dFileName);
  return self;
}

- (IOSByteArray *)toByteArray {
  JavaIoByteArrayOutputStream *out = create_JavaIoByteArrayOutputStream_initWithInt_(ComJhonjuPs3netsrvServerCommandsReadDirEntryCommand_RESULT_LENGTH);
  JavaLangThrowable *__primaryException1 = nil;
  @try {
    [out writeWithByteArray:ComJhonjuPs3netsrvServerUtilsUtils_longToBytesBEWithLong_(self->aFileSize_)];
    [out writeWithByteArray:ComJhonjuPs3netsrvServerUtilsUtils_shortToBytesBEWithShort_(self->bFileNameLength_)];
    [out writeWithInt:cIsDirectory_ ? 1 : 0];
    if (dFileName_ != nil) {
      [out writeWithByteArray:[dFileName_ java_getBytesWithCharset:JreLoadStatic(JavaNioCharsetStandardCharsets, UTF_8)]];
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

- (void)dealloc {
  RELEASE_(dFileName_);
  [super dealloc];
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, NULL, 0x1, -1, -1, -1, -1, -1, -1 },
    { NULL, NULL, 0x1, -1, 0, -1, -1, -1, -1 },
    { NULL, "[B", 0x1, -1, -1, 1, -1, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  #pragma clang diagnostic ignored "-Wundeclared-selector"
  methods[0].selector = @selector(init);
  methods[1].selector = @selector(initWithLong:withShort:withBoolean:withNSString:);
  methods[2].selector = @selector(toByteArray);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "aFileSize_", "J", .constantValue.asLong = 0, 0x11, -1, -1, -1, -1 },
    { "bFileNameLength_", "S", .constantValue.asLong = 0, 0x11, -1, -1, -1, -1 },
    { "cIsDirectory_", "Z", .constantValue.asLong = 0, 0x11, -1, -1, -1, -1 },
    { "dFileName_", "LNSString;", .constantValue.asLong = 0, 0x11, -1, -1, -1, -1 },
  };
  static const void *ptrTable[] = { "JSZLNSString;", "LJavaIoIOException;", "LComJhonjuPs3netsrvServerCommandsReadDirEntryCommand;" };
  static const J2ObjcClassInfo _ComJhonjuPs3netsrvServerCommandsReadDirEntryCommand_ReadDirEntryResult = { "ReadDirEntryResult", "com.jhonju.ps3netsrv.server.commands", ptrTable, methods, fields, 7, 0xa, 3, 4, 2, -1, -1, -1, -1 };
  return &_ComJhonjuPs3netsrvServerCommandsReadDirEntryCommand_ReadDirEntryResult;
}

@end

void ComJhonjuPs3netsrvServerCommandsReadDirEntryCommand_ReadDirEntryResult_init(ComJhonjuPs3netsrvServerCommandsReadDirEntryCommand_ReadDirEntryResult *self) {
  NSObject_init(self);
  self->aFileSize_ = ComJhonjuPs3netsrvServerCommandsAbstractCommand_EMPTY_SIZE;
  self->bFileNameLength_ = ComJhonjuPs3netsrvServerCommandsReadDirEntryCommand_EMPTY_FILE_NAME_LENGTH;
  self->cIsDirectory_ = false;
  JreStrongAssign(&self->dFileName_, nil);
}

ComJhonjuPs3netsrvServerCommandsReadDirEntryCommand_ReadDirEntryResult *new_ComJhonjuPs3netsrvServerCommandsReadDirEntryCommand_ReadDirEntryResult_init() {
  J2OBJC_NEW_IMPL(ComJhonjuPs3netsrvServerCommandsReadDirEntryCommand_ReadDirEntryResult, init)
}

ComJhonjuPs3netsrvServerCommandsReadDirEntryCommand_ReadDirEntryResult *create_ComJhonjuPs3netsrvServerCommandsReadDirEntryCommand_ReadDirEntryResult_init() {
  J2OBJC_CREATE_IMPL(ComJhonjuPs3netsrvServerCommandsReadDirEntryCommand_ReadDirEntryResult, init)
}

void ComJhonjuPs3netsrvServerCommandsReadDirEntryCommand_ReadDirEntryResult_initWithLong_withShort_withBoolean_withNSString_(ComJhonjuPs3netsrvServerCommandsReadDirEntryCommand_ReadDirEntryResult *self, jlong aFileSize, jshort bFileNameLength, jboolean cIsDirectory, NSString *dFileName) {
  NSObject_init(self);
  self->aFileSize_ = aFileSize;
  self->bFileNameLength_ = bFileNameLength;
  self->cIsDirectory_ = cIsDirectory;
  JreStrongAssign(&self->dFileName_, dFileName);
}

ComJhonjuPs3netsrvServerCommandsReadDirEntryCommand_ReadDirEntryResult *new_ComJhonjuPs3netsrvServerCommandsReadDirEntryCommand_ReadDirEntryResult_initWithLong_withShort_withBoolean_withNSString_(jlong aFileSize, jshort bFileNameLength, jboolean cIsDirectory, NSString *dFileName) {
  J2OBJC_NEW_IMPL(ComJhonjuPs3netsrvServerCommandsReadDirEntryCommand_ReadDirEntryResult, initWithLong_withShort_withBoolean_withNSString_, aFileSize, bFileNameLength, cIsDirectory, dFileName)
}

ComJhonjuPs3netsrvServerCommandsReadDirEntryCommand_ReadDirEntryResult *create_ComJhonjuPs3netsrvServerCommandsReadDirEntryCommand_ReadDirEntryResult_initWithLong_withShort_withBoolean_withNSString_(jlong aFileSize, jshort bFileNameLength, jboolean cIsDirectory, NSString *dFileName) {
  J2OBJC_CREATE_IMPL(ComJhonjuPs3netsrvServerCommandsReadDirEntryCommand_ReadDirEntryResult, initWithLong_withShort_withBoolean_withNSString_, aFileSize, bFileNameLength, cIsDirectory, dFileName)
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(ComJhonjuPs3netsrvServerCommandsReadDirEntryCommand_ReadDirEntryResult)
