//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: ./app/src/main/java/com/jhonju/ps3netsrv/server/commands/IResult.java
//

#include "J2ObjC_source.h"
#include "com/jhonju/ps3netsrv/server/commands/IResult.h"

#if __has_feature(objc_arc)
#error "com/jhonju/ps3netsrv/server/commands/IResult must not be compiled with ARC (-fobjc-arc)"
#endif

@interface ComJhonjuPs3netsrvServerCommandsIResult : NSObject

@end

@implementation ComJhonjuPs3netsrvServerCommandsIResult

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, "[B", 0x401, -1, -1, 0, -1, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  #pragma clang diagnostic ignored "-Wundeclared-selector"
  methods[0].selector = @selector(toByteArray);
  #pragma clang diagnostic pop
  static const void *ptrTable[] = { "LJavaIoIOException;" };
  static const J2ObjcClassInfo _ComJhonjuPs3netsrvServerCommandsIResult = { "IResult", "com.jhonju.ps3netsrv.server.commands", ptrTable, methods, NULL, 7, 0x609, 1, 0, -1, -1, -1, -1, -1 };
  return &_ComJhonjuPs3netsrvServerCommandsIResult;
}

@end

J2OBJC_INTERFACE_TYPE_LITERAL_SOURCE(ComJhonjuPs3netsrvServerCommandsIResult)
