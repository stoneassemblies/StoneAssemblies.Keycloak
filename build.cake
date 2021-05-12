#addin "Cake.Docker"
#addin "Cake.FileHelpers"

#load "config.cake"

using System.Text.RegularExpressions;

var target = Argument("target", "Pack");
var buildConfiguration = Argument("Configuration", "Release");

using System.Net;
using System.Net.Sockets;

// var adapter = NetworkInformation.NetworkInterface.GetAllNetworkInterfaces().FirstOrDefault(i => i.Name == "Wi-Fi");
// var properties = adapter.GetIPProperties();

string localIpAddress;
using (var socket = new Socket(AddressFamily.InterNetwork, SocketType.Dgram, 0))
{
    socket.Connect("8.8.8.8", 65530);
    var endPoint = socket.LocalEndPoint as IPEndPoint;
    localIpAddress = endPoint.Address.ToString();
}

var dockerRepositoryProxy = EnvironmentVariable("DOCKER_REPOSITORY_PROXY") ?? $"mcr.microsoft.com";
var dockerRepository = EnvironmentVariable("DOCKER_REPOSITORY") ?? string.Empty;
var dockerRepositoryUsername = EnvironmentVariable("DOCKER_REPOSITORY_USERNAME") ?? string.Empty;
var dockerRepositoryPassword = EnvironmentVariable("DOCKER_REPOSITORY_PASSWORD") ?? string.Empty;

var nugetRepositoryProxy = EnvironmentVariable("NUGET_REPOSITORY_PROXY") ?? $"https://api.nuget.org/v3/index.json";
var nugetRepository = EnvironmentVariable("NUGET_REPOSITORY");
var nugetApiKey = EnvironmentVariable("NUGET_API_KEY");

var DockerRepositoryPrefix = string.IsNullOrWhiteSpace(dockerRepository) ? string.Empty : dockerRepository + "/";


Task("UpdateVersion")
  .Does(() => 
  {
      StartProcess("dotnet", new ProcessSettings
      {
          Arguments = new ProcessArgumentBuilder()
            .Append("gitversion")
            .Append("/output")
            .Append("buildserver")
            .Append("/nofetch")
            .Append("/updateassemblyinfo")
      });

      IEnumerable<string> redirectedStandardOutput;
      StartProcess("dotnet", new ProcessSettings
      {
          Arguments = new ProcessArgumentBuilder()
            .Append("gitversion")
            .Append("/output")
            .Append("json")
            .Append("/nofetch"),
          RedirectStandardOutput = true
      }, out redirectedStandardOutput);

      NuGetVersionV2 = redirectedStandardOutput.FirstOrDefault(s => s.Contains("NuGetVersionV2")).Split(':')[1].Trim(',', ' ', '"');

      System.Xml.XmlDocument pomDocument = new System.Xml.XmlDocument();
      pomDocument.Load("pom.xml");
      pomDocument.DocumentElement["version"].InnerText = NuGetVersionV2;
      pomDocument.Save("pom.xml");
});

Task("Restore")
  .Does(() => 
	{
	      StartProcess("mvn", new ProcessSettings
	      {
	          Arguments = new ProcessArgumentBuilder()
	          .Append("dependency:resolve")
	      });
	});

Task("Build")
  .IsDependentOn("UpdateVersion")
  .IsDependentOn("Restore")
  .Does(() => 
	{
		EnsureDirectoryExists("target");
		CleanDirectory("target");
		
		EnsureDirectoryExists("output");
		CleanDirectory("output");
		
		EnsureDirectoryExists("output/jar");
		CleanDirectory("output/jar");

		StartProcess("mvn", new ProcessSettings
		{
		  Arguments = new ProcessArgumentBuilder()
		    .Append("package")
		});

		CopyFiles("target/*.jar", "output/jar");
	}); 

Task("Publish")
  .IsDependentOn("Build")
  .Does(() => 
  {
      var jarFileName = System.IO.Path.GetFullPath($"output/jar/stoneassemblies-keycloak-{NuGetVersionV2}-jar-with-dependencies.jar");
      var mavenRepositoryUrl = "https://pkgs.dev.azure.com/alexfdezsauco/_packaging/maven/maven/v1";
      var mavenRepositoryName = "maven";

      // TODO: Add parameters.
    	StartProcess("mvn", new ProcessSettings
      {
        Arguments = new ProcessArgumentBuilder()
          .Append("deploy:deploy-file")
          .Append($"-Durl={mavenRepositoryUrl}")
          .Append($"-DrepositoryId={repositoryName}")
          .Append($"-Dfile={jarFileName}")
      });
  });

Task("Pack")
  .IsDependentOn("Build")
  .Does(() => 
  {
  });

RunTarget(target);