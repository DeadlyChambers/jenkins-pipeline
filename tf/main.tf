provider "aws" {
  region = "us-east-1"

  default_tags {
    tags = {
      "ds:app" : "jenkins"
      "ds:env" : "ops"
    }
  }
}

locals {
  #hosted_zone     = "Z0978659ZF19AZNAKB5U"
  #acm_cert_arn    = "arn:aws:acm:us-east-1:${data.aws_caller_identity.current.account_id}:certificate/2d347d0b-dab1-4139-aa9c-31730e6e6d99 "
  domain_name     = "datassential.com"
  vpc_id          = "vpc-0c912ef621e20711f"
  subnet_tag_name = "tag:ds:subnet"
  subdomain_name  = "jenkins"
  tags = {
    "ds:app" : "jenkins"
    "ds:env" : "ops"
  }
}
# data "aws_route53_zone" "selected" {
#   name         = "${domain_name}."
#   private_zone = true
# }
data "aws_network_interface" "all" {

  filter {
    name   = "tag:Name"
    values = ["jenkins-eni"]
  }
}

# data "aws_vpc" "default" {
#   default = true
# }



data "aws_subnets" "all" {
  filter {
    name   = local.subnet_tag_name
    values = ["default"]
  }
}
# data "aws_route53_zone" "this" {
#   name = local.domain_name
# }

# module "log_bucket" {
#   source  = "terraform-aws-modules/s3-bucket/aws"
#   version = "~> 3.0"
#
#   bucket                         = "logs-${random_pet.this.id}"
#   acl                            = "log-delivery-write"
#   force_destroy                  = true
#   attach_elb_log_delivery_policy = true
# }

# module "acm" {
#   source  = "terraform-aws-modules/acm/aws"
#   version = "~> 3.0"

#   domain_name = local.domain_name # trimsuffix(data.aws_route53_zone.this.name, ".")
#   zone_id     = data.aws_route53_zone.this.id
# }

resource "aws_eip" "this" {
  count = length(data.aws_subnets.all.ids)

  vpc = true
}

# module "lb" {
#  source = "terraform-aws-modules/alb/aws"

#  create_lb = false
#  # ... omitted
# }

##################################################################
# Network Load Balancer with Elastic IPs attached
##################################################################
module "nlb" {
  source   = "terraform-aws-modules/alb/aws"
  internal = false
  name     = "complete-nlb-${local.subdomain_name}"
  #enable_deletion_protection = true
  load_balancer_type = "network"
  vpc_id             = local.vpc_id
  #   Use `subnets` if you don't want to attach EIPs
  #   subnets = tolist(data.aws_subnet_ids.all.ids)
  #   Use `subnet_mapping` to attach EIPs
  subnet_mapping = [for i, eip in aws_eip.this : { allocation_id : eip.id, subnet_id : tolist(data.aws_subnets.all.ids)[i] }]
  #   # See notes in README (ref: https://github.com/terraform-providers/terraform-provider-aws/issues/7987)
  #   access_logs = {
  #     bucket = module.log_bucket.s3_bucket_id
  #   }

  #  TLS
  https_listeners = [
    {
      port               = 443
      protocol           = "TLS"
      certificate_arn    = local.acm_cert_arn
      target_group_index = 0

    },
  ]
  #  TCP_UDP, UDP, TCP
  http_tcp_listeners = [
    {
      port               = 80
      protocol           = "TCP"
      target_group_index = 1

    }
  ]

  target_groups = [
    {
      name                      = "nlb-tg-http-${local.subdomain_name}"
      target_type               = "ip"
      backend_protocol          = "TCP"
      backend_port              = 80
      vpc_id                    = local.vpc_id
      associate_with_private_ip = data.aws_network_interface.all.private_ip
      tags_all                  = local.tags

    },

    {
      name                      = "nlb-tg-https-${local.subdomain_name}"
      backend_protocol          = "TCP"
      backend_port              = 443
      target_type               = "ip"
      vpc                       = local.vpc_id
      associate_with_private_ip = data.aws_network_interface.all.private_ip
      tags_all                  = local.tags
    }
  ]

}

resource "aws_lb_target_group_attachment" "attach_https" {
  target_id = data.aws_network_interface.all.private_ip
  #count            = toset(module.nlb.target_group_arns)
  count            = length(module.nlb.target_group_arns)
  target_group_arn = module.nlb.target_group_arns[count.index]
  #target_group_arn = aws_lb_target_group.tg_https.arn
  depends_on = [module.nlb.lb_id]
}

resource "aws_route53_record" "jenkins" {
  zone_id = local.hosted_zone
  name    = "jenkins.${local.domain_name}"
  type    = "A"
  #ttl     = "300"
  alias {
    evaluate_target_health = false
    name                   = module.nlb.lb_dns_name
    zone_id                = module.nlb.lb_zone_id
  }
}

