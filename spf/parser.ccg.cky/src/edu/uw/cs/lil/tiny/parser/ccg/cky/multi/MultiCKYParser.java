/*******************************************************************************
 * UW SPF - The University of Washington Semantic Parsing Framework
 * <p>
 * Copyright (C) 2013 Yoav Artzi
 * <p>
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * <p>
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 ******************************************************************************/
package edu.uw.cs.lil.tiny.parser.ccg.cky.multi;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.uw.cs.lil.tiny.ccg.categories.Category;
import edu.uw.cs.lil.tiny.ccg.categories.ICategoryServices;
import edu.uw.cs.lil.tiny.ccg.lexicon.ILexiconImmutable;
import edu.uw.cs.lil.tiny.explat.DistributedExperiment;
import edu.uw.cs.lil.tiny.explat.IResourceRepository;
import edu.uw.cs.lil.tiny.explat.ParameterizedExperiment;
import edu.uw.cs.lil.tiny.explat.ParameterizedExperiment.Parameters;
import edu.uw.cs.lil.tiny.explat.resources.IResourceObjectCreator;
import edu.uw.cs.lil.tiny.explat.resources.usage.ResourceUsage;
import edu.uw.cs.lil.tiny.parser.ISentenceLexiconGenerator;
import edu.uw.cs.lil.tiny.parser.ccg.cky.AbstractCKYParser;
import edu.uw.cs.lil.tiny.parser.ccg.cky.CKYBinaryParsingRule;
import edu.uw.cs.lil.tiny.parser.ccg.cky.SimpleWordSkippingLexicalGenerator;
import edu.uw.cs.lil.tiny.parser.ccg.cky.chart.AbstractCellFactory;
import edu.uw.cs.lil.tiny.parser.ccg.cky.chart.Cell;
import edu.uw.cs.lil.tiny.parser.ccg.cky.chart.Chart;
import edu.uw.cs.lil.tiny.parser.ccg.model.IDataItemModel;
import edu.uw.cs.lil.tiny.parser.ccg.rules.IBinaryParseRule;
import edu.uw.cs.lil.tiny.utils.concurrency.ITinyExecutor;
import edu.uw.cs.utils.collections.CollectionUtils;
import edu.uw.cs.utils.filter.IFilter;
import edu.uw.cs.utils.log.ILogger;
import edu.uw.cs.utils.log.LoggerFactory;
import edu.uw.cs.utils.log.thread.LoggingRunnable;

/**
 * Multi threaded CKY parser. Work is distributed on the level of span splits.
 * 
 * @author Yoav Artzi
 * @param <MR>
 *            type of semantics.
 */
public class MultiCKYParser<MR> extends AbstractCKYParser<MR> {
	private static final ILogger	LOG	= LoggerFactory
												.create(MultiCKYParser.class);
	private final ITinyExecutor		executor;
	private final boolean			preChartPruning;
	
	private MultiCKYParser(int maxNumberOfCellsInSpan,
			List<CKYBinaryParsingRule<MR>> binaryParseRules,
			List<ISentenceLexiconGenerator<MR>> sentenceLexiconGenerators,
			ISentenceLexiconGenerator<MR> wordSkippingLexicalGenerator,
			ICategoryServices<MR> categoryServices, ITinyExecutor executor,
			boolean pruneLexicalCells, boolean preChartPruning,
			IFilter<Category<MR>> completeParseFilter) {
		super(maxNumberOfCellsInSpan, binaryParseRules,
				sentenceLexiconGenerators, wordSkippingLexicalGenerator,
				categoryServices, pruneLexicalCells, completeParseFilter);
		this.executor = executor;
		this.preChartPruning = preChartPruning;
	}
	
	@Override
	protected Chart<MR> doParse(IFilter<MR> pruningFilter,
			IDataItemModel<MR> model, Chart<MR> chart, int numTokens,
			AbstractCellFactory<MR> cellFactory,
			List<ILexiconImmutable<MR>> lexicons) {
		
		LOG.debug("Starting a multi-threaded CKY parse (chart already populated)");
		
		// Generate all triplets of span and splits <begin,end,split> and
		// store them in a set. Notice the added dummy splits for lexical jobs.
		final Map<SpanPair, Set<SplitTriplet>> splits = new HashMap<SpanPair, Set<SplitTriplet>>();
		for (int len = 0; len < numTokens; len++) {
			for (int begin = 0; begin < numTokens - len; begin++) {
				final HashSet<SplitTriplet> triplets = new HashSet<SplitTriplet>();
				splits.put(new SpanPair(begin, begin + len), triplets);
				// Add dummy split for the lexical job
				triplets.add(new SplitTriplet(begin, begin + len, -1));
				for (int split = 0; split < len; split++) {
					triplets.add(new SplitTriplet(begin, begin + len, split));
				}
			}
		}
		
		// Create a map to hold all completed spans, index them by both
		// start and end points (so most will appear in the map twice).
		final Map<Integer, Set<SpanPair>> completedSpanBegins = new HashMap<Integer, Set<SpanPair>>();
		final Map<Integer, Set<SpanPair>> completedSpanEnds = new HashMap<Integer, Set<SpanPair>>();
		for (int i = 0; i < numTokens; ++i) {
			completedSpanBegins.put(i, new HashSet<SpanPair>());
			completedSpanEnds.put(i, new HashSet<SpanPair>());
		}
		
		// Lock
		final SpanLock lock = new SpanLock(numTokens);
		
		// Create the listener
		final Listener listener = new Listener(completedSpanBegins,
				completedSpanEnds, pruningFilter, model, chart, numTokens,
				cellFactory, lock, splits);
		
		try {
			// Need to sync over splits, in case the jobs will complete before
			// we get to wait on it. Plus, to wait we need to lock it using
			// sync.
			synchronized (splits) {
				// Add all lexical jobs to start the parsing process
				LOG.debug("Creating initial lexical jobs");
				
				// Create all lexical jobs
				for (int i = 0; i < numTokens; i++) {
					for (int j = i; j < numTokens; j++) {
						executor.execute(new LexicalJob(cellFactory, chart,
								listener, lock, model, new SpanPair(i, j),
								lexicons, pruningFilter));
					}
				}
				
				// Wait for the set of triplets to be empty, i.e., all spans are
				// processed
				executor.wait(splits);
			}
		} catch (final InterruptedException e) {
			throw new IllegalStateException(e);
		}
		
		return chart;
	}
	
	public static class Builder<MR> {
		
		private final List<CKYBinaryParsingRule<MR>>		binaryParseRules			= new LinkedList<CKYBinaryParsingRule<MR>>();
		
		private final ICategoryServices<MR>					categoryServices;
		
		private final IFilter<Category<MR>>					completeParseFilter;
		
		private final ITinyExecutor							executor;
		
		/** The maximum number of cells allowed in each span */
		private int											maxNumberOfCellsInSpan		= 50;
		
		/**
		 * Pre-chart pruning creates a further approximation of the packed chart
		 * which influences non-maximal children. It does mean that worker
		 * threads will take chart span locks for shorter periods.
		 */
		private boolean										preChartPruning				= false;
		
		private boolean										pruneLexicalCells			= false;
		
		private final List<ISentenceLexiconGenerator<MR>>	sentenceLexicalGenerators	= new LinkedList<ISentenceLexiconGenerator<MR>>();
		
		private ISentenceLexiconGenerator<MR>				wordSkippingLexicalGenerator;
		
		public Builder(ICategoryServices<MR> categoryServices,
				ITinyExecutor executor,
				IFilter<Category<MR>> completeParseFilter) {
			this.categoryServices = categoryServices;
			this.executor = executor;
			this.completeParseFilter = completeParseFilter;
			this.wordSkippingLexicalGenerator = new SimpleWordSkippingLexicalGenerator<MR>(
					categoryServices);
		}
		
		public Builder<MR> addBinaryParseRule(CKYBinaryParsingRule<MR> rule) {
			binaryParseRules.add(rule);
			return this;
		}
		
		public Builder<MR> addSentenceLexicalGenerator(
				ISentenceLexiconGenerator<MR> generator) {
			sentenceLexicalGenerators.add(generator);
			return this;
		}
		
		public MultiCKYParser<MR> build() {
			return new MultiCKYParser<MR>(maxNumberOfCellsInSpan,
					binaryParseRules, sentenceLexicalGenerators,
					wordSkippingLexicalGenerator, categoryServices, executor,
					pruneLexicalCells, preChartPruning, completeParseFilter);
		}
		
		public Builder<MR> setMaxNumberOfCellsInSpan(int maxNumberOfCellsInSpan) {
			this.maxNumberOfCellsInSpan = maxNumberOfCellsInSpan;
			return this;
		}
		
		public Builder<MR> setPreChartPruning(boolean preChartPruning) {
			this.preChartPruning = preChartPruning;
			return this;
		}
		
		public Builder<MR> setPruneLexicalCells(boolean pruneLexicalCells) {
			this.pruneLexicalCells = pruneLexicalCells;
			return this;
		}
		
		public Builder<MR> setWordSkippingLexicalGenerator(
				ISentenceLexiconGenerator<MR> wordSkippingLexicalGenerator) {
			this.wordSkippingLexicalGenerator = wordSkippingLexicalGenerator;
			return this;
		}
	}
	
	public static class Creator<MR> implements
			IResourceObjectCreator<MultiCKYParser<MR>> {
		
		private String	type;
		
		public Creator() {
			this("parser.cky.multi");
		}
		
		public Creator(String type) {
			this.type = type;
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public MultiCKYParser<MR> create(Parameters params,
				IResourceRepository repo) {
			final Builder<MR> builder = new Builder<MR>(
					(ICategoryServices<MR>) repo
							.getResource(DistributedExperiment.CATEGORY_SERVICES_RESOURCE),
					(ITinyExecutor) repo
							.getResource(ParameterizedExperiment.EXECUTOR_RESOURCE),
					(IFilter<Category<MR>>) repo.getResource(params
							.get("parseFilter")));
			
			if (params.contains("beam")) {
				builder.setMaxNumberOfCellsInSpan(params.getAsInteger("beam"));
			}
			
			if (params.contains("preChartPruning")) {
				builder.setPreChartPruning(params
						.getAsBoolean("preChartPruning"));
			}
			
			if (params.contains("pruneLexicalCells")) {
				builder.setPruneLexicalCells(params
						.getAsBoolean("pruneLexicalCells"));
			}
			
			if (params.contains("wordSkippingLexGen")) {
				builder.setWordSkippingLexicalGenerator((ISentenceLexiconGenerator<MR>) repo
						.getResource(params.get("wordSkippingLexGen")));
			}
			
			for (final String id : params.getSplit("generators")) {
				builder.addSentenceLexicalGenerator((ISentenceLexiconGenerator<MR>) repo
						.getResource(id));
			}
			
			for (final String id : params.getSplit("rules")) {
				builder.addBinaryParseRule(new CKYBinaryParsingRule<MR>(
						(IBinaryParseRule<MR>) repo.getResource(id)));
			}
			
			for (final String id : params.getSplit("ckyRules")) {
				builder.addBinaryParseRule((CKYBinaryParsingRule<MR>) repo
						.getResource(id));
			}
			
			return builder.build();
		}
		
		@Override
		public String type() {
			return type;
		}
		
		@Override
		public ResourceUsage usage() {
			return ResourceUsage
					.builder(type, MultiCKYParser.class)
					.addParam("parseFilter", IFilter.class,
							"Filter to determine complete parses.")
					.addParam("beam", Integer.class,
							"Beam to use for cell pruning (default: 50).")
					.addParam("preChartPruning", Boolean.class,
							"Prune categories before adding to the chart (default: false)")
					.addParam("pruneLexicalCells", Boolean.class,
							"Prune lexical entries similarly to conventional categories (default: false)")
					.addParam("wordSkippingLexGen",
							ISentenceLexiconGenerator.class,
							"Lexical generator for word skipping (defaults to simple skipping).")
					.addParam("generators", ISentenceLexiconGenerator.class,
							"List of dynamic sentence lexical generators.")
					.addParam("rules", IBinaryParseRule.class,
							"Binary parsing rules.").build();
		}
		
	}
	
	private abstract class AbstractJob extends LoggingRunnable {
		protected final AbstractCellFactory<MR>	cellFactory;
		protected final Chart<MR>				chart;
		protected final Listener				listener;
		protected final SpanLock				lock;
		protected final IDataItemModel<MR>		model;
		protected final SplitTriplet			split;
		
		public AbstractJob(AbstractCellFactory<MR> cellFactory,
				Chart<MR> chart, Listener listener, SpanLock lock,
				IDataItemModel<MR> model, SplitTriplet split) {
			this.cellFactory = cellFactory;
			this.chart = chart;
			this.listener = listener;
			this.lock = lock;
			this.model = model;
			this.split = split;
		}
	}
	
	private class LexicalJob extends AbstractJob {
		
		private final List<ILexiconImmutable<MR>>	lexicons;
		private final IFilter<MR>					pruningFilter;
		
		public LexicalJob(AbstractCellFactory<MR> cellFactory, Chart<MR> chart,
				Listener listener, SpanLock lock, IDataItemModel<MR> model,
				SpanPair span, List<ILexiconImmutable<MR>> lexicons,
				IFilter<MR> pruningFilter) {
			super(cellFactory, chart, listener, lock, model, new SplitTriplet(
					span.begin, span.end, -1));
			this.lexicons = lexicons;
			this.pruningFilter = pruningFilter;
		}
		
		@Override
		public void loggedRun() {
			LOG.debug("%s Lexical job started", split.span);
			
			final List<Cell<MR>> newCells = generateLexicalCells(split.begin,
					split.end, chart, lexicons, model);
			
			LOG.debug("%s: %d new lexical cells", split.span, newCells.size());
			
			if (pruneLexicalCells) {
				// Hard pruning
				CollectionUtils.filterInPlace(newCells,
						new IFilter<Cell<MR>>() {
							@Override
							public boolean isValid(Cell<MR> e) {
								return !prune(pruningFilter, e.getCategroy());
							}
						});
				LOG.debug("%s: %d new lexical cells passed hard pruning",
						split.span, newCells.size());
			}
			
			// Add all the valid cells under a span lock
			lock.lock(split.begin, split.end);
			for (final Cell<MR> newCell : newCells) {
				chart.add(newCell, model);
			}
			lock.unlock(split.begin, split.end);
			
			LOG.debug("%s: Lexical job completed, tried to add %d entries",
					split.span, newCells.size());
			
			// Signal the job is complete
			listener.jobComplete(this);
		}
	}
	
	private class Listener {
		private final IndexLock							adjacentLock;
		private final AbstractCellFactory<MR>			cellFactory;
		private final Chart<MR>							chart;
		private final Map<Integer, Set<SpanPair>>		completedSpanBegins;
		private final Map<Integer, Set<SpanPair>>		completedSpanEnds;
		private final SpanLock							lock;
		private final IDataItemModel<MR>				model;
		private final int								numTokens;
		private final IFilter<MR>						pruningFilter;
		private final Map<SpanPair, Set<SplitTriplet>>	splits;
		
		public Listener(Map<Integer, Set<SpanPair>> completedSpanBegins,
				Map<Integer, Set<SpanPair>> completedSpanEnds,
				IFilter<MR> pruningFilter, IDataItemModel<MR> model,
				Chart<MR> chart, int numTokens,
				AbstractCellFactory<MR> cellFactory, SpanLock lock,
				Map<SpanPair, Set<SplitTriplet>> splits) {
			this.completedSpanBegins = completedSpanBegins;
			this.completedSpanEnds = completedSpanEnds;
			this.pruningFilter = pruningFilter;
			this.model = model;
			this.chart = chart;
			this.numTokens = numTokens;
			this.cellFactory = cellFactory;
			this.lock = lock;
			this.splits = splits;
			this.adjacentLock = new IndexLock(numTokens);
		}
		
		public void jobComplete(AbstractJob job) {
			final boolean spanComplete;
			
			synchronized (splits) {
				// Remove split from splits set
				final Set<SplitTriplet> spanSplits = splits.get(job.split.span);
				spanSplits.remove(job.split);
				
				// Case all splits for this span are processed
				spanComplete = spanSplits.isEmpty();
				if (spanComplete) {
					// Remove span
					splits.remove(job.split.span);
					
					if (splits.isEmpty()) {
						// Case all splits processed, notify
						splits.notifyAll();
					}
				}
			}
			
			if (spanComplete) {
				LOG.debug("Span completed: %s", job.split.span);
				
				// Iterate over all neighboring completed spans, and
				// create and queue split jobs with them. For each
				// pair of spans only one of them gets here while the other
				// already exists, so there's no duplicate work. This is why
				// it's critical to access this part under the same lock in
				// which the newly added span is added to the being and ends
				// maps.
				
				// Block I: Queue jobs with spans on the left
				
				// Lock to stop all spans that are adjacent to the current
				// span on the left from getting into block II
				if (job.split.begin > 0) {
					adjacentLock.lock(job.split.begin - 1);
					for (final SpanPair leftSpan : completedSpanEnds
							.get(job.split.begin - 1)) {
						executor.execute(new SplitJob(pruningFilter, model,
								chart, numTokens, cellFactory,
								new SplitTriplet(leftSpan.begin, job.split.end,
										leftSpan.end - leftSpan.begin), lock,
								this));
					}
				}
				// Add the beginning index of the span to the completed
				// map
				completedSpanBegins.get(job.split.begin).add(job.split.span);
				if (job.split.begin > 0) {
					adjacentLock.unlock(job.split.begin - 1);
				}
				
				// Block II: Queue jobs with spans on the right
				
				// Lock to stop all spans that are adjacent to the current
				// span on the right from getting into block I
				if (job.split.end < numTokens - 1) {
					adjacentLock.lock(job.split.end);
					for (final SpanPair rightSpan : completedSpanBegins
							.get(job.split.end + 1)) {
						executor.execute(new SplitJob(pruningFilter, model,
								chart, numTokens, cellFactory,
								new SplitTriplet(job.split.begin,
										rightSpan.end, job.split.end
												- job.split.begin), lock, this));
					}
				}
				// Add the end index of the span to the completed map
				completedSpanEnds.get(job.split.end).add(job.split.span);
				if (job.split.end < numTokens - 1) {
					adjacentLock.unlock(job.split.end);
				}
			}
		}
	}
	
	private static class SpanPair {
		final int	begin;
		final int	end;
		final int	hashCode;
		
		public SpanPair(int begin, int end) {
			this.begin = begin;
			this.end = end;
			this.hashCode = calcHashCode();
		}
		
		public int calcHashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + begin;
			result = prime * result + end;
			return result;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			final SpanPair other = (SpanPair) obj;
			if (begin != other.begin) {
				return false;
			}
			if (end != other.end) {
				return false;
			}
			return true;
		}
		
		@Override
		public int hashCode() {
			return hashCode;
		}
		
		@Override
		public String toString() {
			return "(" + begin + ", " + end + ")";
		}
	}
	
	private class SplitJob extends AbstractJob {
		
		private final int			numTokens;
		private final IFilter<MR>	pruningFilter;
		
		public SplitJob(IFilter<MR> pruningFilter, IDataItemModel<MR> model,
				Chart<MR> chart, int numTokens,
				AbstractCellFactory<MR> cellFactory, SplitTriplet split,
				SpanLock lock, Listener listener) {
			super(cellFactory, chart, listener, lock, model, split);
			this.pruningFilter = pruningFilter;
			this.numTokens = numTokens;
			LOG.debug("Created split job for %s", split);
		}
		
		@Override
		public void loggedRun() {
			LOG.debug("%s: Split job started", split);
			
			final List<Cell<MR>> newCells = preChartPruning ? processSplitAndPrune(
					split.begin, split.end, split.split, chart, cellFactory,
					numTokens, pruningFilter, chart.getBeamSize(), model)
					: processSplit(split.begin, split.end, split.split, chart,
							cellFactory, numTokens, pruningFilter, model);
			
			LOG.debug("%s: %d new cells", split, newCells.size());
			
			// Add all the valid cells under a span lock
			lock.lock(split.begin, split.end);
			for (final Cell<MR> newCell : newCells) {
				chart.add(newCell, model);
			}
			lock.unlock(split.begin, split.end);
			
			LOG.debug("%s: Split job completed", split);
			
			// Signal the job is complete
			listener.jobComplete(this);
		}
	}
	
	private static class SplitTriplet {
		final int		begin;
		final int		end;
		final int		hashCode;
		final SpanPair	span;
		final int		split;
		
		public SplitTriplet(int begin, int end, int split) {
			this.begin = begin;
			this.end = end;
			this.split = split;
			this.span = new SpanPair(begin, end);
			this.hashCode = calcHashCode();
		}
		
		public int calcHashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + begin;
			result = prime * result + end;
			result = prime * result + split;
			return result;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			final SplitTriplet other = (SplitTriplet) obj;
			if (begin != other.begin) {
				return false;
			}
			if (end != other.end) {
				return false;
			}
			if (split != other.split) {
				return false;
			}
			return true;
		}
		
		@Override
		public int hashCode() {
			return hashCode;
		}
		
		@Override
		public String toString() {
			return "(" + begin + ", " + end + ")[" + split + "]";
		}
		
	}
	
}
