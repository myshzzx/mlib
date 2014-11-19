

namespace java mysh.cluster.rpc.thrift

service TClusterService {

  binary invokeSvMethod(1:string methodName, 2:binary params)

}

