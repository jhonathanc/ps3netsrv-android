//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: ./app/src/main/java/com/jhonju/ps3netsrv/server/commands/ICommand.java
//

#include "J2ObjC_source.h"
#include "com/jhonju/ps3netsrv/server/commands/ICommand.h"

#if __has_feature(objc_arc)
#error "com/jhonju/ps3netsrv/server/commands/ICommand must not be compiled with ARC (-fobjc-arc)"
#endif

@interface ComJhonjuPs3netsrvServerCommandsICommand : NSObject

@end

@implementation ComJhonjuPs3netsrvServerCommandsICommand

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, "V", 0x401, -1, -1, 0, -1, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  #pragma clang diagnostic ignored "-Wundeclared-selector"
  methods[0].selector = @selector(executeTask);
  #pragma clang diagnostic pop
  static const void *ptrTable[] = { "LJavaIoIOException;LComJhonjuPs3netsrvServerExceptionsPS3NetSrvException;" };
  static const J2ObjcClassInfo _ComJhonjuPs3netsrvServerCommandsICommand = { "ICommand", "com.jhonju.ps3netsrv.server.commands", ptrTable, methods, NULL, 7, 0x609, 1, 0, -1, -1, -1, -1, -1 };
  return &_ComJhonjuPs3netsrvServerCommandsICommand;
}

@end

J2OBJC_INTERFACE_TYPE_LITERAL_SOURCE(ComJhonjuPs3netsrvServerCommandsICommand)
