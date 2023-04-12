//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: ./app/src/main/java/com/jhonju/ps3netsrv/server/enums/ENetIsoCommand.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_ComJhonjuPs3netsrvServerEnumsENetIsoCommand")
#ifdef RESTRICT_ComJhonjuPs3netsrvServerEnumsENetIsoCommand
#define INCLUDE_ALL_ComJhonjuPs3netsrvServerEnumsENetIsoCommand 0
#else
#define INCLUDE_ALL_ComJhonjuPs3netsrvServerEnumsENetIsoCommand 1
#endif
#undef RESTRICT_ComJhonjuPs3netsrvServerEnumsENetIsoCommand

#if __has_feature(nullability)
#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wnullability"
#pragma GCC diagnostic ignored "-Wnullability-completeness"
#endif

#if !defined (ComJhonjuPs3netsrvServerEnumsENetIsoCommand_) && (INCLUDE_ALL_ComJhonjuPs3netsrvServerEnumsENetIsoCommand || defined(INCLUDE_ComJhonjuPs3netsrvServerEnumsENetIsoCommand))
#define ComJhonjuPs3netsrvServerEnumsENetIsoCommand_

#define RESTRICT_JavaLangEnum 1
#define INCLUDE_JavaLangEnum 1
#include "java/lang/Enum.h"

@class IOSObjectArray;

typedef NS_ENUM(NSUInteger, ComJhonjuPs3netsrvServerEnumsENetIsoCommand_Enum) {
  ComJhonjuPs3netsrvServerEnumsENetIsoCommand_Enum_NETISO_CMD_OPEN_FILE = 0,
  ComJhonjuPs3netsrvServerEnumsENetIsoCommand_Enum_NETISO_CMD_READ_FILE_CRITICAL = 1,
  ComJhonjuPs3netsrvServerEnumsENetIsoCommand_Enum_NETISO_CMD_READ_CD_2048_CRITICAL = 2,
  ComJhonjuPs3netsrvServerEnumsENetIsoCommand_Enum_NETISO_CMD_READ_FILE = 3,
  ComJhonjuPs3netsrvServerEnumsENetIsoCommand_Enum_NETISO_CMD_CREATE_FILE = 4,
  ComJhonjuPs3netsrvServerEnumsENetIsoCommand_Enum_NETISO_CMD_WRITE_FILE = 5,
  ComJhonjuPs3netsrvServerEnumsENetIsoCommand_Enum_NETISO_CMD_OPEN_DIR = 6,
  ComJhonjuPs3netsrvServerEnumsENetIsoCommand_Enum_NETISO_CMD_READ_DIR_ENTRY = 7,
  ComJhonjuPs3netsrvServerEnumsENetIsoCommand_Enum_NETISO_CMD_DELETE_FILE = 8,
  ComJhonjuPs3netsrvServerEnumsENetIsoCommand_Enum_NETISO_CMD_MKDIR = 9,
  ComJhonjuPs3netsrvServerEnumsENetIsoCommand_Enum_NETISO_CMD_RMDIR = 10,
  ComJhonjuPs3netsrvServerEnumsENetIsoCommand_Enum_NETISO_CMD_READ_DIR_ENTRY_V2 = 11,
  ComJhonjuPs3netsrvServerEnumsENetIsoCommand_Enum_NETISO_CMD_STAT_FILE = 12,
  ComJhonjuPs3netsrvServerEnumsENetIsoCommand_Enum_NETISO_CMD_GET_DIR_SIZE = 13,
  ComJhonjuPs3netsrvServerEnumsENetIsoCommand_Enum_NETISO_CMD_READ_DIR = 14,
  ComJhonjuPs3netsrvServerEnumsENetIsoCommand_Enum_NETISO_CMD_CUSTOM_0 = 15,
};

@interface ComJhonjuPs3netsrvServerEnumsENetIsoCommand : JavaLangEnum {
 @public
  jint value_;
}

#pragma mark Public

+ (ComJhonjuPs3netsrvServerEnumsENetIsoCommand *)valueOfWithInt:(jint)command;

+ (ComJhonjuPs3netsrvServerEnumsENetIsoCommand *)valueOfWithNSString:(NSString *)name;

+ (IOSObjectArray *)values;

#pragma mark Package-Private

- (ComJhonjuPs3netsrvServerEnumsENetIsoCommand_Enum)toNSEnum;

@end

J2OBJC_STATIC_INIT(ComJhonjuPs3netsrvServerEnumsENetIsoCommand)

/*! INTERNAL ONLY - Use enum accessors declared below. */
FOUNDATION_EXPORT ComJhonjuPs3netsrvServerEnumsENetIsoCommand *ComJhonjuPs3netsrvServerEnumsENetIsoCommand_values_[];

inline ComJhonjuPs3netsrvServerEnumsENetIsoCommand *ComJhonjuPs3netsrvServerEnumsENetIsoCommand_get_NETISO_CMD_OPEN_FILE(void);
J2OBJC_ENUM_CONSTANT(ComJhonjuPs3netsrvServerEnumsENetIsoCommand, NETISO_CMD_OPEN_FILE)

inline ComJhonjuPs3netsrvServerEnumsENetIsoCommand *ComJhonjuPs3netsrvServerEnumsENetIsoCommand_get_NETISO_CMD_READ_FILE_CRITICAL(void);
J2OBJC_ENUM_CONSTANT(ComJhonjuPs3netsrvServerEnumsENetIsoCommand, NETISO_CMD_READ_FILE_CRITICAL)

inline ComJhonjuPs3netsrvServerEnumsENetIsoCommand *ComJhonjuPs3netsrvServerEnumsENetIsoCommand_get_NETISO_CMD_READ_CD_2048_CRITICAL(void);
J2OBJC_ENUM_CONSTANT(ComJhonjuPs3netsrvServerEnumsENetIsoCommand, NETISO_CMD_READ_CD_2048_CRITICAL)

inline ComJhonjuPs3netsrvServerEnumsENetIsoCommand *ComJhonjuPs3netsrvServerEnumsENetIsoCommand_get_NETISO_CMD_READ_FILE(void);
J2OBJC_ENUM_CONSTANT(ComJhonjuPs3netsrvServerEnumsENetIsoCommand, NETISO_CMD_READ_FILE)

inline ComJhonjuPs3netsrvServerEnumsENetIsoCommand *ComJhonjuPs3netsrvServerEnumsENetIsoCommand_get_NETISO_CMD_CREATE_FILE(void);
J2OBJC_ENUM_CONSTANT(ComJhonjuPs3netsrvServerEnumsENetIsoCommand, NETISO_CMD_CREATE_FILE)

inline ComJhonjuPs3netsrvServerEnumsENetIsoCommand *ComJhonjuPs3netsrvServerEnumsENetIsoCommand_get_NETISO_CMD_WRITE_FILE(void);
J2OBJC_ENUM_CONSTANT(ComJhonjuPs3netsrvServerEnumsENetIsoCommand, NETISO_CMD_WRITE_FILE)

inline ComJhonjuPs3netsrvServerEnumsENetIsoCommand *ComJhonjuPs3netsrvServerEnumsENetIsoCommand_get_NETISO_CMD_OPEN_DIR(void);
J2OBJC_ENUM_CONSTANT(ComJhonjuPs3netsrvServerEnumsENetIsoCommand, NETISO_CMD_OPEN_DIR)

inline ComJhonjuPs3netsrvServerEnumsENetIsoCommand *ComJhonjuPs3netsrvServerEnumsENetIsoCommand_get_NETISO_CMD_READ_DIR_ENTRY(void);
J2OBJC_ENUM_CONSTANT(ComJhonjuPs3netsrvServerEnumsENetIsoCommand, NETISO_CMD_READ_DIR_ENTRY)

inline ComJhonjuPs3netsrvServerEnumsENetIsoCommand *ComJhonjuPs3netsrvServerEnumsENetIsoCommand_get_NETISO_CMD_DELETE_FILE(void);
J2OBJC_ENUM_CONSTANT(ComJhonjuPs3netsrvServerEnumsENetIsoCommand, NETISO_CMD_DELETE_FILE)

inline ComJhonjuPs3netsrvServerEnumsENetIsoCommand *ComJhonjuPs3netsrvServerEnumsENetIsoCommand_get_NETISO_CMD_MKDIR(void);
J2OBJC_ENUM_CONSTANT(ComJhonjuPs3netsrvServerEnumsENetIsoCommand, NETISO_CMD_MKDIR)

inline ComJhonjuPs3netsrvServerEnumsENetIsoCommand *ComJhonjuPs3netsrvServerEnumsENetIsoCommand_get_NETISO_CMD_RMDIR(void);
J2OBJC_ENUM_CONSTANT(ComJhonjuPs3netsrvServerEnumsENetIsoCommand, NETISO_CMD_RMDIR)

inline ComJhonjuPs3netsrvServerEnumsENetIsoCommand *ComJhonjuPs3netsrvServerEnumsENetIsoCommand_get_NETISO_CMD_READ_DIR_ENTRY_V2(void);
J2OBJC_ENUM_CONSTANT(ComJhonjuPs3netsrvServerEnumsENetIsoCommand, NETISO_CMD_READ_DIR_ENTRY_V2)

inline ComJhonjuPs3netsrvServerEnumsENetIsoCommand *ComJhonjuPs3netsrvServerEnumsENetIsoCommand_get_NETISO_CMD_STAT_FILE(void);
J2OBJC_ENUM_CONSTANT(ComJhonjuPs3netsrvServerEnumsENetIsoCommand, NETISO_CMD_STAT_FILE)

inline ComJhonjuPs3netsrvServerEnumsENetIsoCommand *ComJhonjuPs3netsrvServerEnumsENetIsoCommand_get_NETISO_CMD_GET_DIR_SIZE(void);
J2OBJC_ENUM_CONSTANT(ComJhonjuPs3netsrvServerEnumsENetIsoCommand, NETISO_CMD_GET_DIR_SIZE)

inline ComJhonjuPs3netsrvServerEnumsENetIsoCommand *ComJhonjuPs3netsrvServerEnumsENetIsoCommand_get_NETISO_CMD_READ_DIR(void);
J2OBJC_ENUM_CONSTANT(ComJhonjuPs3netsrvServerEnumsENetIsoCommand, NETISO_CMD_READ_DIR)

inline ComJhonjuPs3netsrvServerEnumsENetIsoCommand *ComJhonjuPs3netsrvServerEnumsENetIsoCommand_get_NETISO_CMD_CUSTOM_0(void);
J2OBJC_ENUM_CONSTANT(ComJhonjuPs3netsrvServerEnumsENetIsoCommand, NETISO_CMD_CUSTOM_0)

FOUNDATION_EXPORT ComJhonjuPs3netsrvServerEnumsENetIsoCommand *ComJhonjuPs3netsrvServerEnumsENetIsoCommand_valueOfWithInt_(jint command);

FOUNDATION_EXPORT IOSObjectArray *ComJhonjuPs3netsrvServerEnumsENetIsoCommand_values(void);

FOUNDATION_EXPORT ComJhonjuPs3netsrvServerEnumsENetIsoCommand *ComJhonjuPs3netsrvServerEnumsENetIsoCommand_valueOfWithNSString_(NSString *name);

FOUNDATION_EXPORT ComJhonjuPs3netsrvServerEnumsENetIsoCommand *ComJhonjuPs3netsrvServerEnumsENetIsoCommand_fromOrdinal(NSUInteger ordinal);

J2OBJC_TYPE_LITERAL_HEADER(ComJhonjuPs3netsrvServerEnumsENetIsoCommand)

#endif


#if __has_feature(nullability)
#pragma clang diagnostic pop
#endif
#pragma pop_macro("INCLUDE_ALL_ComJhonjuPs3netsrvServerEnumsENetIsoCommand")
