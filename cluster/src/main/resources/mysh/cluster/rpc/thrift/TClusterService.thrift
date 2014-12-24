

namespace java mysh.cluster.rpc.thrift

service TClusterService {

  binary invokeSvMethod(1:string ns, 2:string method, 3:binary params)

}

