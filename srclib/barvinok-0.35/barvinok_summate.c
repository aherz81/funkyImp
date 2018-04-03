#include <assert.h>
#include <isl/stream.h>
#include <barvinok/barvinok.h>
#include <barvinok/options.h>
#include <barvinok/util.h>
#include "verify.h"

struct options {
	struct verify_options    *verify;
};

ISL_ARGS_START(struct options, options_args)
ISL_ARG_CHILD(struct options, verify, NULL,
	&verify_options_args, "verification")
ISL_ARGS_END

ISL_ARG_DEF(options, struct options, options_args)

struct verify_point_sum {
	struct verify_point_data vpd;
	isl_pw_qpolynomial *pwqp;
	isl_pw_qpolynomial *sum;

	isl_pw_qpolynomial *fixed;
	isl_qpolynomial *manual;
};

static int manual_sum(__isl_take isl_point *pnt, void *user)
{
	struct verify_point_sum *vps = (struct verify_point_sum *) user;
	isl_qpolynomial *qp;

	qp = isl_pw_qpolynomial_eval(isl_pw_qpolynomial_copy(vps->fixed), pnt);
	vps->manual = isl_qpolynomial_add(vps->manual, qp);

	return 0;
}

static int verify_point(__isl_take isl_point *pnt, void *user)
{
	struct verify_point_sum *vps = (struct verify_point_sum *) user;
	int i;
	int ok;
	unsigned nvar;
	unsigned nparam;
	isl_int v;
	isl_space *space;
	isl_set *dom;
	isl_qpolynomial *eval;
	int r;
	FILE *out = vps->vpd.options->print_all ? stdout : stderr;

	vps->vpd.n--;

	isl_int_init(v);
	vps->fixed = isl_pw_qpolynomial_copy(vps->pwqp);
	nparam = isl_pw_qpolynomial_dim(vps->sum, isl_dim_param);
	for (i = 0; i < nparam; ++i) {
		isl_point_get_coordinate(pnt, isl_dim_param, i, &v);
		vps->fixed = isl_pw_qpolynomial_fix_dim(vps->fixed,
						    	isl_dim_param, i, v);
	}

	eval = isl_pw_qpolynomial_eval(isl_pw_qpolynomial_copy(vps->sum),
					isl_point_copy(pnt));

	space = isl_pw_qpolynomial_get_domain_space(vps->pwqp);
	vps->manual = isl_qpolynomial_zero_on_domain(space);
	dom = isl_pw_qpolynomial_domain(isl_pw_qpolynomial_copy(vps->fixed));
	r = isl_set_foreach_point(dom, &manual_sum, user);
	isl_set_free(dom);
	if (r < 0)
		goto error;

	nvar = isl_set_dim(dom, isl_dim_set);
	vps->manual = isl_qpolynomial_project_domain_on_params(vps->manual);

	ok = isl_qpolynomial_plain_is_equal(eval, vps->manual);

	if (vps->vpd.options->print_all || !ok) {
		isl_ctx *ctx = isl_pw_qpolynomial_get_ctx(vps->pwqp);
		isl_printer *p = isl_printer_to_file(ctx, out);
		fprintf(out, "sum(");
		for (i = 0; i < nparam; ++i) {
			if (i)
				fprintf(out, ", ");
			isl_point_get_coordinate(pnt, isl_dim_param, i, &v);
			isl_int_print(out, v, 0);
		}
		fprintf(out, ") = ");
		p = isl_printer_print_qpolynomial(p, eval);
		fprintf(out, ", sum(EP) = ");
		p = isl_printer_print_qpolynomial(p, vps->manual);
		if (ok)
			fprintf(out, ". OK\n");
		else
			fprintf(out, ". NOT OK\n");
		isl_printer_free(p);
	} else if ((vps->vpd.n % vps->vpd.s) == 0) {
		printf("o");
		fflush(stdout);
	}

	if (0) {
error:
		ok = 0;
	}
	isl_qpolynomial_free(vps->manual);
	isl_pw_qpolynomial_free(vps->fixed);
	isl_qpolynomial_free(eval);
	isl_int_clear(v);
	isl_point_free(pnt);

	if (!ok)
		vps->vpd.error = 1;

	if (vps->vpd.options->continue_on_error)
		ok = 1;

	return (vps->vpd.n >= 1 && ok) ? 0 : -1;
}

static int verify(__isl_keep isl_pw_qpolynomial *pwqp,
	__isl_take isl_pw_qpolynomial *sum, struct verify_options *options)
{
	struct verify_point_sum vps = { { options } };
	isl_set *context;
	int r;

	vps.pwqp = pwqp;
	vps.sum = sum;

	context = isl_pw_qpolynomial_domain(isl_pw_qpolynomial_copy(sum));
	context = verify_context_set_bounds(context, options);

	r = verify_point_data_init(&vps.vpd, context);

	if (r == 0)
		isl_set_foreach_point(context, verify_point, &vps);
	if (vps.vpd.error)
		r = -1;

	isl_set_free(context);

	verify_point_data_fini(&vps.vpd);

	return r;
}

int main(int argc, char **argv)
{
    int i;
    int result = 0;
    isl_ctx *ctx;
    isl_space *dim;
    isl_pw_qpolynomial *pwqp;
    isl_pw_qpolynomial *sum;
    struct isl_stream *s;
    struct options *options = options_new_with_defaults();

    argc = options_parse(options, argc, argv, ISL_ARG_ALL);
    ctx = isl_ctx_alloc_with_options(&options_args, options);

    s = isl_stream_new_file(ctx, stdin);
    pwqp = isl_stream_read_pw_qpolynomial(s);

    if (options->verify->verify) {
	isl_space *dim = isl_pw_qpolynomial_get_space(pwqp);
	unsigned total = isl_space_dim(dim, isl_dim_all);
	isl_space_free(dim);
	verify_options_set_range(options->verify, total);
    }

    sum = isl_pw_qpolynomial_sum(isl_pw_qpolynomial_copy(pwqp));
    if (options->verify->verify)
	result = verify(pwqp, sum, options->verify);
    else {
	isl_printer *p = isl_printer_to_file(ctx, stdout);
	p = isl_printer_print_pw_qpolynomial(p, sum);
	p = isl_printer_end_line(p);
	isl_printer_free(p);
    }
    isl_pw_qpolynomial_free(sum);
    isl_pw_qpolynomial_free(pwqp);

    isl_stream_free(s);
    isl_ctx_free(ctx);
    return result;
}
