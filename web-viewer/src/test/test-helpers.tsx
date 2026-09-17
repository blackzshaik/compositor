import React from 'react';
import { PreviewCatalog, PreviewItem } from '../types/preview';
import { PreviewConfigProvider } from '../context/PreviewConfigContext';
import { PreviewCatalogProvider } from '../context/PreviewCatalogContext';

export const MOCK_PREVIEW_ITEM: PreviewItem = {
  id: 'samples.sample-app:Greeting.kt:GreetingPreview',
  module: 'samples:sample-app',
  definition: {
    functionName: 'GreetingPreview',
    packageName: 'com.compositor.sample',
    line: 25,
    filePath: 'samples/sample-app/src/main/java/com/compositor/sample/Greeting.kt',
    parameters: {
      name: 'Greeting Light',
      group: 'Greetings',
      showBackground: true,
      fontScale: 1.0,
    },
  },
  status: 'Rendered',
  durationMs: 45,
  lastRenderedAt: Date.now(),
  imageUrl: 'http://localhost:3001/api/preview/latest.png',
};

export const MOCK_PREVIEW_ITEM_2: PreviewItem = {
  id: 'samples.sample-app:Greeting.kt:GreetingDarkPreview',
  module: 'samples:sample-app',
  definition: {
    functionName: 'GreetingDarkPreview',
    packageName: 'com.compositor.sample',
    line: 35,
    filePath: 'samples/sample-app/src/main/java/com/compositor/sample/Greeting.kt',
    parameters: {
      name: 'Greeting Dark',
      group: 'Greetings',
      uiMode: 32,
      fontScale: 1.0,
    },
  },
  status: 'Rendered',
  durationMs: 50,
  lastRenderedAt: Date.now(),
  imageUrl: 'http://localhost:3001/api/previews/samples.sample-app%3AGreeting.kt%3AGreetingDarkPreview/image',
};

export const MOCK_CATALOG: PreviewCatalog = {
  previews: {
    [MOCK_PREVIEW_ITEM.id]: MOCK_PREVIEW_ITEM,
    [MOCK_PREVIEW_ITEM_2.id]: MOCK_PREVIEW_ITEM_2,
  },
  byModule: {
    'samples:sample-app': [MOCK_PREVIEW_ITEM.id, MOCK_PREVIEW_ITEM_2.id],
  },
  byFile: {
    'samples/sample-app/src/main/java/com/compositor/sample/Greeting.kt': [
      MOCK_PREVIEW_ITEM.id,
      MOCK_PREVIEW_ITEM_2.id,
    ],
  },
  byGroup: {
    Greetings: [MOCK_PREVIEW_ITEM.id, MOCK_PREVIEW_ITEM_2.id],
  },
  totalCount: 2,
};

export const TestProviders: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  return (
    <PreviewConfigProvider>
      <PreviewCatalogProvider>{children}</PreviewCatalogProvider>
    </PreviewConfigProvider>
  );
};
