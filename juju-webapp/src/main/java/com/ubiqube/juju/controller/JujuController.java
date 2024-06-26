package com.ubiqube.juju.controller;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ubiqube.etsi.mano.service.juju.entities.JujuCloud;
import com.ubiqube.etsi.mano.service.juju.entities.JujuCredential;
import com.ubiqube.etsi.mano.service.juju.entities.JujuMetadata;
import com.ubiqube.etsi.mano.service.juju.entities.JujuRegion;
import com.ubiqube.juju.JujuException;
import com.ubiqube.juju.service.ProcessResult;
import com.ubiqube.juju.service.WorkspaceService;

import jakarta.validation.constraints.NotNull;

@Validated
@RestController("/")
@SuppressWarnings("static-method")
public class JujuController {

	private static final Logger LOG = LoggerFactory.getLogger(JujuController.class);
	private final WorkspaceService ws;

	public JujuController(final WorkspaceService ws) {
		this.ws = ws;
	}

	@PostMapping(value = "/cloud", produces = "application/json")
	public ResponseEntity<String> addCloud(@RequestBody @NotNull final JujuCloud cloud) {
		LOG.info("calling POST /cloud with name={}, cloud type={}\n{}", cloud.getName(), cloud.getType(), cloud);
		final String cloudString = genCloudYml(cloud);
		final InputStream is = new ByteArrayInputStream(cloudString.getBytes());
		final String filename = "openstack-play.yaml";
		ws.pushPayload(is, filename);
		LOG.info("{}: Call Install ", ws.getId());
		final ProcessResult res = ws.addCloud(cloud.getName(), filename);
		LOG.info("process result"+res);
		LOG.info("{}: add-cloud done.", ws.getId());
		LOG.info(res.getStdout());
		LOG.error(res.getErrout());
		return new ResponseEntity<>(res.getErrout(),HttpStatus.CREATED);
	}

	@GetMapping(value = "/cloud", produces = "application/json")
	public ResponseEntity<String> clouds() {
		LOG.info("calling GET /cloud");
		final ProcessResult res = ws.clouds();
		LOG.info("process result"+res);
		LOG.info(res.getStdout());
		LOG.error(res.getErrout());
		return ResponseEntity.ok(res.getStdout());
	}

	@GetMapping(value = "/cloud/{name}", produces = "application/json")
	public ResponseEntity<String> cloudDetail(@PathVariable("name") @NotNull final String name) {
		LOG.info("calling GET /cloud");
		final ProcessResult res = ws.cloudDetail(name);
		LOG.info("process result"+res);
		LOG.info(res.getStdout());
		LOG.error(res.getErrout());
		if(res.getExitCode()==1){
			return ResponseEntity.ok(res.getErrout());
		}
		return ResponseEntity.ok(res.getStdout());
	}

	@DeleteMapping(value = "/cloud/{name}", produces = "application/json")
	public ResponseEntity<String> removeCloud(@PathVariable("name") @NotNull final String name) {
		LOG.info("calling DELETE /cloud with name: {}", name);
		final ProcessResult res = ws.removeCloud(name);
		LOG.info("process result"+res);
		LOG.info(res.getStdout());
		LOG.error(res.getErrout());
		return ResponseEntity.ok(res.getErrout());
	}

	@PostMapping(value = "/credential", produces = "application/json")
	public ResponseEntity<String> addCredential(@RequestBody @NotNull final JujuCloud cloud) {
		LOG.info("calling POST /credential with object: {}", cloud);
		final String credentialString = genCredentialYml(cloud);
		final InputStream is = new ByteArrayInputStream(credentialString.getBytes());
		LOG.info("{}: Deploying payload.", ws.getId());
		final String filename = "mycreds.yaml";
		ws.pushPayload(is, filename);
		final ProcessResult res = ws.addCredential(cloud.getName(), filename);
		LOG.info("process result"+res);
		if(res.getExitCode()==0) {
			return new ResponseEntity<>(res.getStdout(),HttpStatus.CREATED);
		}
		return new ResponseEntity<>(res.getErrout(),HttpStatus.CREATED);
	}

	@GetMapping(value = "/credential", produces = "application/json")
	public ResponseEntity<String> credentials() {
		LOG.info("calling GET /credential");
		final ProcessResult res = ws.credentials();
		LOG.info("process result"+res);
		LOG.info(res.getStdout());
		LOG.error(res.getErrout());
		return ResponseEntity.ok(res.getStdout());
	}

	@GetMapping(value = "/credential/{cloudname}/{name}", produces = "application/json")
	public ResponseEntity<String> credentialDetails(@PathVariable("cloudname") @NotNull final String cloudname, @PathVariable("name") final String name) {
		LOG.info("calling GET /credentialDetails with cloudname:{} and name=:{}", cloudname, name);
		final ProcessResult res1 = ws.cloudDetail(cloudname);
		LOG.info("process result1"+res1);
		if (res1.getExitCode() == 1) {
			return ResponseEntity.ok(res1.getErrout());
		} else {
			final ProcessResult res2 = ws.credentialDetail(cloudname, name);
			LOG.info("process result2" + res2);
			LOG.info(res2.getStdout());
			LOG.error(res2.getErrout());
			if (res2.getStdout().isEmpty()) {
				return ResponseEntity.ok(res2.getErrout());
			}
			return ResponseEntity.ok(res2.getStdout());
		}
	}

	@PutMapping(value = "/credential", produces = "application/json")
	public ResponseEntity<String> updateCredential(@RequestBody @NotNull final JujuCloud cloud) {
		LOG.info("calling PUT /credential with object: {}", cloud);
		final String credentialString = genCredentialYml(cloud);
		final InputStream is = new ByteArrayInputStream(credentialString.getBytes());
		LOG.info("{}: Deploying payload.", ws.getId());
		final String filename = "mycreds.yaml";
		ws.pushPayload(is, filename);
		final ProcessResult res = ws.updateCredential(cloud.getName(), filename);
		LOG.info("process result"+res);
		if(res.getExitCode()==1){
			return new ResponseEntity<>(res.getErrout(),HttpStatus.CREATED);
		}
		return new ResponseEntity<>(res.getErrout(),HttpStatus.CREATED);
	}

	@DeleteMapping(value = "/credential/{cloudname}/{name}", produces = "application/json")
	public ResponseEntity<String> removeCredential(@PathVariable("cloudname") @NotNull final String cloudname, @PathVariable("name") final String name) {
		LOG.info("calling DELETE /credential with cloudname:{} and name=:{}", cloudname, name);
		final ProcessResult res = ws.removeCredential(cloudname, name);
		LOG.info("process result"+res);
		LOG.info(res.getStdout());
		LOG.error(res.getErrout());
		return ResponseEntity.ok(res.getErrout());
	}

	public ResponseEntity<String> addMetadata(@RequestParam("path") @NotNull final String path,
											  @RequestParam("imageId") @NotNull final String imageId, @RequestParam("osSeries") @NotNull final String osSeries,
											  @RequestParam("region") @NotNull final String region, @RequestParam("osAuthUrl") @NotNull final String osAuthUrl) {
		LOG.info("calling POST /metadata");
		final ProcessResult res = ws.genMetadata(path, imageId, osSeries, region, osAuthUrl);
		LOG.info(res.getStdout());
		LOG.error(res.getErrout());
		return ResponseEntity.ok(res.getStdout());
	}

	@PostMapping(value = "/metadata", produces = "application/json")
	public ResponseEntity<String> genMetadata(@RequestBody @NotNull final JujuMetadata meta) {
		LOG.info("calling POST /metadata with object: {}", meta);
		final ProcessResult res = ws.genMetadata(meta.getPath(), meta.getImageId(), meta.getOsSeries(), meta.getRegionName(), meta.getOsAuthUrl());
		LOG.info(res.getStdout());
		LOG.error(res.getErrout());
		return ResponseEntity.ok(res.getStdout());
	}

	public ResponseEntity<String> addController2(@RequestParam("imageId") @NotNull final String imageId,
												 @RequestParam("osSeries") @NotNull final String osSeries, @RequestParam("constraints") @NotNull final String constraints,
												 @RequestParam("cloudname") @NotNull final String cloudname, @RequestParam("controllername") @NotNull final String controllername,
												 @RequestParam("region") @NotNull final String region, @RequestParam("networkId") @NotNull final String networkId) {
		LOG.info("post /controller");
		final ProcessResult res = ws.addController(imageId, osSeries, constraints, cloudname, controllername, region, networkId);
		LOG.info(res.getStdout());
		LOG.error(res.getErrout());
		return ResponseEntity.ok(res.getStdout());
	}

	@PostMapping(value = "/controller/{cloudname}", produces = "application/json")
	public ResponseEntity<String> addController(@PathVariable("cloudname") @NotNull final String cloudname, @RequestBody @NotNull final JujuMetadata controller) {
		LOG.info("post /controller");
		final ProcessResult res = ws.addController(cloudname, controller);
		LOG.info("Proceess result"+res);
		LOG.info(res.getStdout());
		LOG.error(res.getErrout());
		return new ResponseEntity<>(res.getErrout(),HttpStatus.CREATED);
	}

	@GetMapping(value = "/controller", produces = "application/json")
	public ResponseEntity<String> controllers() {
		LOG.info("get /controllers");
		final ProcessResult res = ws.controllers();
		LOG.info("Proceess result"+res);
		LOG.info(res.getStdout());
		LOG.error(res.getErrout());
		return ResponseEntity.ok(res.getStdout());
	}

	@GetMapping(value = "/controller/{controllername}", produces = "application/json")
	public ResponseEntity<String> controllerDetail(@PathVariable("controllername") @NotNull final String controllername) {
		LOG.info("get /showController/{}", controllername);
		final ProcessResult res = ws.showController(controllername);
		LOG.info("Proceess result"+res);
		LOG.info(res.getStdout());
		LOG.error(res.getErrout());
		if (res.getExitCode() == 0) {
			return ResponseEntity.ok(res.getStdout());
		}
		return ResponseEntity.ok(res.getErrout());
	}

	@DeleteMapping(value = "/controller/{controllername}", produces = "application/json")
	public ResponseEntity<String> removeController(@PathVariable("controllername") @NotNull final String controllername) {
		LOG.info("delete /remove-controller/{}", controllername);
		final ProcessResult res = ws.removeController(controllername);
		LOG.info("Proceess result1 "+res);
		LOG.info(res.getStdout());
		LOG.error(res.getErrout());
		return ResponseEntity.ok(res.getErrout());
	}

	@PostMapping(value = "/model/{name}", produces = "application/json")
	public ResponseEntity<String> addModel(@PathVariable("name") @NotNull final String name) {
		LOG.info("post /add-model");
		final ProcessResult res = ws.addModel(name);
		LOG.info("Proceess result1 "+res);
		LOG.info(res.getStdout());
		LOG.error(res.getErrout());
		return new ResponseEntity<>(res.getErrout(),HttpStatus.CREATED);
	}

	@GetMapping(value = "/model", produces = "application/json")
	public ResponseEntity<String> model() {
		LOG.info("get /models");
		final ProcessResult res = ws.model();
		LOG.info("Proceess result1 "+res);
		LOG.info(res.getStdout());
		LOG.error(res.getErrout());
		return ResponseEntity.ok(res.getStdout());
	}

	@GetMapping(value = "/model/{modelname}", produces = "application/json")
	public ResponseEntity<String> modelDetail(@PathVariable("modelname") @NotNull final String modelname) {
		LOG.info("get /showController/{}", modelname);
		final ProcessResult res = ws.showModel(modelname);
		LOG.info("Proceess result"+res);
		LOG.info(res.getStdout());
		LOG.error(res.getErrout());
		if(res.getExitCode()==0) {
			return ResponseEntity.ok(res.getStdout());
		}
		return ResponseEntity.ok(res.getErrout());
	}

	@DeleteMapping(value = "/model/{name}", produces = "application/json")
	public ResponseEntity<String> removeModel(@PathVariable("name") @NotNull final String name) {
		LOG.info("delete /destroy-model/{}", name);
		final ProcessResult res = ws.removeModel(name);
		LOG.info("Proceess result1 "+res);
		LOG.info(res.getStdout());
		LOG.error(res.getErrout());
		return ResponseEntity.ok(res.getErrout());
	}

	@PostMapping(value = "/application/{charm}/{name}", produces = "application/json")
	public ResponseEntity<String> deployApp(@PathVariable("charm") @NotNull final String charm, @PathVariable("name") @NotNull final String name) {
		LOG.info("post /deploy/{}/{}", charm, name);
		final ProcessResult res = ws.deployApp(charm, name);
		LOG.info("Proceess result1 "+res);
		LOG.info(res.getStdout());
		LOG.error(res.getErrout());
		return new ResponseEntity<>(res.getErrout(),HttpStatus.CREATED);
	}

	@GetMapping(value = "/application/{name}", produces = "application/json")
	public ResponseEntity<String> application(@PathVariable("name") @NotNull final String name) {
		LOG.info("calling /show-application/{}", name);
		final ProcessResult res = ws.application(name);
		LOG.info("Proceess result1 "+res);
		LOG.info(res.getStdout());
		LOG.error(res.getErrout());
		if (res.getExitCode() == 0) {
			return ResponseEntity.ok(res.getStdout());
		}
		return ResponseEntity.ok(res.getErrout());
	}

	@DeleteMapping(value = "/application/{name}", produces = "application/json")
	public ResponseEntity<String> removeApplication(@PathVariable("name") @NotNull final String name) {
		LOG.info("calling /remove-application/{}", name);
		final ProcessResult res = ws.removeApplication(name);
		LOG.info("Proceess result1 "+res);
		LOG.info(res.getStdout());
		LOG.error(res.getErrout());
		if (res.getExitCode() == 0) {
			return ResponseEntity.ok(res.getStdout());
		}
		return ResponseEntity.ok(res.getErrout());
	}

	@GetMapping(value = "/status", produces = "application/json")
	public ResponseEntity<String> status() {
		LOG.info("calling /status");
		final ProcessResult res = ws.status();
		LOG.info("Proceess result1 "+res);
		LOG.info(res.getStdout());
		LOG.error(res.getErrout());
		return ResponseEntity.ok(res.getStdout());
	}

	@GetMapping(value = "/isk8sready", produces = "application/json")
	public ResponseEntity<Boolean> isK8sReady() {
		LOG.info("calling /isK8sReady");
		boolean result= ws.isK8sReady();
		return ResponseEntity.ok(result);
	}

	@GetMapping(value = "/kubeconfig", produces = "application/json")
	public ResponseEntity<String> getKubeConfig(String vnfid) {
		final ProcessResult res = ws.kubeConfig("kubernetes-control-plane/0");
		return ResponseEntity.ok(res.getStdout());
	}

	@PostMapping(value = "/kubeconfig", produces = "application/json")
	public ResponseEntity<String> addKubeConfig(@RequestBody @NotNull final String filename) {
		LOG.info("post /kubeconfig");
		final ProcessResult res = ws.kubeConfig("kubernetes-control-plane/0");
		LOG.info("Proceess result1 " + res);
		LOG.info(res.getStdout());
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(filename));
			writer.write(res.getStdout());
			writer.close();
		} catch (IOException ioe) {
			return ResponseEntity.ok(ioe.getMessage());
		}
		LOG.error(res.getErrout());
		if (res.getExitCode() == 1) {
			return ResponseEntity.ok(res.getErrout());
		}
		return new ResponseEntity<>(res.getStdout(), HttpStatus.CREATED);
	}

	@PostMapping(value = "/helminstall/{helmName}", produces = "application/json", consumes = { "multipart/form-data" })
	public ResponseEntity<String> helmInstall(@PathVariable("helmName") @NotNull final String helmName,
			@RequestParam("file") MultipartFile tgzfile) {
		LOG.info("post /helminstall");
		try {
			final InputStream is = new ByteArrayInputStream(tgzfile.getBytes());
			final String filename = helmName + ".tgz";
			ws.pushPayload(is, filename);
			final ProcessResult res = ws.helmInstall(helmName, filename);
			LOG.info("Proceess result1 " + res);
			LOG.info(res.getStdout());
			LOG.error(res.getErrout());
			if (res.getExitCode() == 1) {
				return ResponseEntity.ok(res.getErrout());
			}
			return new ResponseEntity<>(res.getStdout(), HttpStatus.OK);
		} catch (IOException e) {
			throw new JujuException(e);
		}
	}

	@PostMapping(value = "/helminstall2/{helmName}", produces = "application/json")
	public ResponseEntity<String> helmInstall2(@PathVariable("helmName") @NotNull final String helmName,
			@RequestBody @NotNull final String filename) {
		LOG.info("post /helminstall2");
		final ProcessResult res = ws.helmInstall(helmName, filename);
		LOG.info("Proceess result1 " + res);
		LOG.info(res.getStdout());
		LOG.error(res.getErrout());
		if (res.getExitCode() == 1) {
			return ResponseEntity.ok(res.getErrout());
		}
		return new ResponseEntity<>(res.getStdout(), HttpStatus.CREATED);
	}

	@GetMapping(value = "/helmlist", produces = "application/json")
	public ResponseEntity<String> helmList() {
		LOG.info("calling /helmlist");
		final ProcessResult res = ws.helmList();
		LOG.info("Proceess result1 " + res);
		LOG.info(res.getStdout());
		LOG.error(res.getErrout());
		if (res.getExitCode() == 1) {
			return ResponseEntity.ok(res.getErrout());
		}
		return ResponseEntity.ok(res.getStdout());
	}

	@DeleteMapping(value = "/helmuninstall/{helmName}", produces = "application/json")
	public ResponseEntity<String> helmUninstall(@PathVariable("helmName") @NotNull final String helmName) {
		LOG.info("delete /helm uninstall");
		final ProcessResult res = ws.helmUninstall(helmName);
		LOG.info("Proceess result1 " + res);
		LOG.info(res.getStdout());
		LOG.error(res.getErrout());
		return ResponseEntity.ok(res.getErrout());
	}

	@PostMapping(value = "/command", produces = "application/json")
	public ResponseEntity<String> command(@RequestBody @NotNull final String cmd) {
		LOG.info("calling /command:" + cmd);
		final ProcessResult res = ws.raw(cmd);
		LOG.info("Proceess result1 " + res);
		LOG.info(res.getStdout());
		LOG.error(res.getErrout());
		return ResponseEntity.ok(res.getStdout());
	}

	private String genCloudYml(final JujuCloud cloud) {
		/*
		 * clouds: openstack-cloud-240: type: openstack auth-types: [userpass] regions:
		 * RegionOne: endpoint: http://10.31.1.240:5000/v3
		 */
		final StringBuilder str = new StringBuilder("clouds:\n");
		str.append("    " + cloud.getName() + ":\n");
		str.append("      type: openstack\n");
		str.append("      auth-types: [userpass]\n");
		str.append("      regions:\n");
		if ((cloud.getRegions() != null) && !cloud.getRegions().isEmpty()) {
			for (final JujuRegion region : cloud.getRegions()) {
				str.append("        " + region.getName() + ":\n");
				str.append("          endpoint: " + region.getEndPoint() + "\n");
			}
		}
		LOG.info("CloudYaml:\n{}", str.toString());
		return str.toString();
	}

	private String genCredentialYml(final JujuCloud cloud) {
		/*
		 * credentials: openstack-cloud-240: # .240 Openstack instance admin: auth-type:
		 * userpass password: 13f83cb78a4f4213 tenant-name: admin username: admin
		 *
		 */
		final JujuCredential credential = cloud.getCredential();
		final StringBuilder str = new StringBuilder("credentials:\n");
		str.append("  " + cloud.getName() + ":\n");
		str.append("    " + credential.getName() + ":\n");
		str.append("      auth-type: " + credential.getAuthType() + "\n");
		str.append("      password: " + credential.getPassword() + "\n");
		str.append("      tenant-name: " + credential.getTenantName() + "\n");
		str.append("      username: " + credential.getUsername() + "\n");
		LOG.info("CredentialYaml:\n{}", str.toString());
		return str.toString();
	}
}